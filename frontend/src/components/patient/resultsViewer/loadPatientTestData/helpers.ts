import {
  PatientData,
  ObsRecord,
  ConceptUuid,
  ConceptRecord,
  ObsMetaInfo,
  OBSERVATION_INTERPRETATION,
} from "../commons";

const PAGE_SIZE = 300;
const CHUNK_PREFETCH_COUNT = 1;

const retrieveFromIterator = <T>(
  iteratorOrIterable: IterableIterator<T>,
  length: number,
): Array<T | undefined> => {
  const iterator = iteratorOrIterable[Symbol.iterator]();
  return Array.from({ length }, () => iterator.next().value);
};

const PATIENT_DATA_CACHE_SIZE = 5;
let patientResultsDataCache: Record<string, [PatientData, number, string]> = {};

/**
 * Adds given user testresults data to a cache
 *
 * @param patientUuid
 * @param data {PatientData}
 * @param indicator UUID of the newest observation
 */
export function addUserDataToCache(
  patientUuid: string,
  data: PatientData,
  indicator: string,
) {
  patientResultsDataCache[patientUuid] = [data, Date.now(), indicator];
  const currentStateEntries = Object.entries(patientResultsDataCache);

  if (currentStateEntries.length > PATIENT_DATA_CACHE_SIZE) {
    currentStateEntries.sort(([, [, dateA]], [, [, dateB]]) => dateB - dateA);

    patientResultsDataCache = Object.fromEntries(
      currentStateEntries.slice(0, PATIENT_DATA_CACHE_SIZE),
    );
  }
}

async function getLatestObsUuid(patientUuid: string): Promise<string> {
  const request = fhirObservationRequests({
    patient: patientUuid,
    category: "laboratory",
    _sort: "-_date",
    _summary: "data",
    _format: "json",
    _count: "1",
  });
  const result = await request.next().value;
  return result?.entry?.[0]?.resource?.id;
}

/**
 * Retrieves cached user testresults data
 * Checks the indicator against the backend while doing so
 *
 * @param { string } patientUuid
 * @param { PatientData } data
 * @param { string } indicator UUID of the newest observation
 */
export function getUserDataFromCache(
  patientUuid: string,
): [PatientData | undefined, Promise<boolean>] {
  const [data] = patientResultsDataCache[patientUuid] || [];

  return [
    data,
    data
      ? getLatestObsUuid(patientUuid).then(
          (obsUuid) => obsUuid !== patientResultsDataCache?.[patientUuid]?.[2],
        )
      : Promise.resolve(true),
  ];
}

/**
 * Iterator
 * @param queries
 */
function* fhirObservationRequests(queries: Record<string, string>) {
  const fhirPathname = `${window.openmrsBase}/ws/fhir2/R4/Observation`;
  const path =
    fhirPathname +
    "?" +
    Object.entries(queries)
      .map(([q, v]) => q + "=" + v)
      .join("&");

  const pathWithPageOffset = (offset) =>
    path + "&_getpagesoffset=" + offset * PAGE_SIZE;
  let offsetCounter = 0;
  while (true) {
    yield fetch(pathWithPageOffset(offsetCounter++)).then((res) => res.json());
  }
}

/**
 * Load all patient testresult observations in parallel
 *
 * @param { string } patientUuid
 * @returns { Promise<Array<ObsRecord>> }
 */
export const loadObsEntries = async (
  patientUuid: string,
): Promise<Array<ObsRecord>> => {
  const requests = fhirObservationRequests({
    patient: patientUuid,
    category: "laboratory",
    _sort: "-_date",
    _summary: "data",
    _format: "json",
    _count: "" + PAGE_SIZE,
  });

  let responses = await Promise.all(
    retrieveFromIterator(requests, CHUNK_PREFETCH_COUNT),
  );

  const total = responses[0].total;

  if (total > CHUNK_PREFETCH_COUNT * PAGE_SIZE) {
    const missingRequestsCount =
      Math.ceil(total / PAGE_SIZE) - CHUNK_PREFETCH_COUNT;
    responses = [
      ...responses,
      ...(await Promise.all(
        retrieveFromIterator(requests, missingRequestsCount),
      )),
    ];
  }

  return responses
    .slice(0, Math.ceil(total / PAGE_SIZE))
    .flatMap((res) => res.entry.map((e) => e.resource));
};

export const getEntryConceptClassUuid = (entry) => entry.code.coding[0].code;

const conceptCache: Record<ConceptUuid, Promise<ConceptRecord>> = {};
/**
 * fetch all concepts for all given observation entries
 */
export function loadPresentConcepts(
  entries: Array<ObsRecord>,
): Promise<Array<ConceptRecord>> {
  return Promise.all(
    [...new Set(entries.map(getEntryConceptClassUuid))].map(
      (conceptUuid) =>
        conceptCache[conceptUuid] ||
        (conceptCache[conceptUuid] = fetch(
          `${window.openmrsBase}/ws/rest/v1/concept/${conceptUuid}?v=full`,
        ).then((res) => res.json())),
    ),
  );
}

/**
 * returns true if no value is null or undefined
 *
 * @param args any
 * @returns {boolean}
 */
export function exist(...args: any[]): boolean {
  for (const y of args) {
    if (y === null || y === undefined) {
      return false;
    }
  }

  return true;
}

export const assessValue =
  (meta: ObsMetaInfo) =>
  (value: string): OBSERVATION_INTERPRETATION => {
    if (isNaN(parseFloat(value))) {
      return "NORMAL";
    }
    const numericValue = parseFloat(value);
    if (exist(meta.hiAbsolute) && numericValue > meta.hiAbsolute) {
      return "OFF_SCALE_HIGH";
    }

    if (exist(meta.hiCritical) && numericValue > meta.hiCritical) {
      return "CRITICALLY_HIGH";
    }

    if (exist(meta.hiNormal) && numericValue > meta.hiNormal) {
      return "HIGH";
    }

    if (exist(meta.lowAbsolute) && numericValue < meta.lowAbsolute) {
      return "OFF_SCALE_LOW";
    }

    if (exist(meta.lowCritical) && numericValue < meta.lowCritical) {
      return "CRITICALLY_LOW";
    }

    if (exist(meta.lowNormal) && numericValue < meta.lowNormal) {
      return "LOW";
    }

    return "NORMAL";
  };

export function extractMetaInformation(
  concepts: Array<ConceptRecord>,
): Record<ConceptUuid, ObsMetaInfo> {
  return Object.fromEntries(
    concepts.map(
      ({
        uuid,
        hiAbsolute,
        hiCritical,
        hiNormal,
        lowAbsolute,
        lowCritical,
        lowNormal,
        units,
        datatype: { display: datatype },
      }) => {
        const meta: ObsMetaInfo = {
          hiAbsolute,
          hiCritical,
          hiNormal,
          lowAbsolute,
          lowCritical,
          lowNormal,
          units,
          datatype,
        };

        if (exist(hiNormal, lowNormal)) {
          meta.range = `${lowNormal} – ${hiNormal}`;
        }

        meta.assessValue = assessValue(meta);

        return [uuid, meta];
      },
    ),
  );
}
