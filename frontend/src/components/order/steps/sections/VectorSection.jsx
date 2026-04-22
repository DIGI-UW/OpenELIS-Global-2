import React, { useState, useEffect, useCallback } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Grid,
  Column,
  Select,
  SelectItem,
  TextInput,
  Toggle,
  Tile,
  Stack,
  Tag,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";

const GROUPS_URL = "/rest/admin/vector/groups/active";
const TRAP_TYPES_URL = "/rest/admin/vector/trap-types";
const SPECIES_URL = "/rest/admin/vector/species";

/**
 * VectorSection — order entry fields for vector surveillance workflow.
 * All values are stored in orderData.sampleOrderItems.environmentalFields
 * under vec* keys, mirroring the LocationSection pattern.
 */
function VectorSection({ orderData, setOrderData, isReadOnly }) {
  const intl = useIntl();

  const envFields = orderData?.sampleOrderItems?.environmentalFields || {};

  const [groups, setGroups] = useState([]);
  const [trapTypes, setTrapTypes] = useState([]);
  const [speciesList, setSpeciesList] = useState([]);

  const updateEnvField = useCallback(
    (key, value) => {
      setOrderData((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          environmentalFields: {
            ...prev.sampleOrderItems?.environmentalFields,
            [key]: value,
          },
        },
      }));
    },
    [setOrderData],
  );

  // Load active groups on mount
  useEffect(() => {
    getFromOpenElisServer(GROUPS_URL, (data) => setGroups(data || []));
  }, []);

  // Load trap types + species when group changes
  useEffect(() => {
    const groupId = envFields.vecOrganismGroupId;
    if (!groupId) {
      setTrapTypes([]);
      setSpeciesList([]);
      return;
    }
    getFromOpenElisServer(`${TRAP_TYPES_URL}?groupId=${groupId}`, (data) =>
      setTrapTypes(data || []),
    );
    getFromOpenElisServer(`${SPECIES_URL}?groupId=${groupId}`, (data) =>
      setSpeciesList(data || []),
    );
  }, [envFields.vecOrganismGroupId]);

  // Derive selected species from speciesList + current vecSpeciesId (no side-effects needed)
  const selectedSpecies =
    speciesList.find((s) => String(s.id) === String(envFields.vecSpeciesId)) ||
    null;

  const lifecycleOptions = (selectedSpecies?.lifecycleStages || []).map(
    (d) => d.dictEntry,
  );

  const handleGroupChange = (e) => {
    const groupId = e.target.value;
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        environmentalFields: {
          ...prev.sampleOrderItems?.environmentalFields,
          vecOrganismGroupId: groupId,
          vecSpeciesId: "",
          vecTrapTypeId: "",
          vecLifecycleStage: "",
          vecPathogensOfInterest: "",
        },
      },
    }));
  };

  const handleSpeciesChange = (e) => {
    const speciesId = e.target.value;
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        environmentalFields: {
          ...prev.sampleOrderItems?.environmentalFields,
          vecSpeciesId: speciesId,
          vecLifecycleStage: "",
        },
      },
    }));
  };

  const poolCount = parseInt(envFields.vecPoolCount) || 0;
  const samplesPerPool = parseInt(envFields.vecSamplesPerPool) || 0;
  const totalSpecimens =
    poolCount > 0 && samplesPerPool > 0 ? poolCount * samplesPerPool : null;

  return (
    <Tile className="order-section">
      <h4 className="section-title">
        <FormattedMessage
          id="workflow.vector"
          defaultMessage="Vector Surveillance"
        />
      </h4>

      <Stack gap={5}>
        {/* Row 1: Organism Group + Species + Trap Type */}
        <Grid narrow>
          <Column lg={5} md={4} sm={4}>
            <Select
              id="vec-group"
              labelText={intl.formatMessage({
                id: "vector.organismGroup",
                defaultMessage: "Organism Group",
              })}
              value={envFields.vecOrganismGroupId || ""}
              onChange={handleGroupChange}
              disabled={isReadOnly}
            >
              <SelectItem value="" text="" />
              {groups.map((g) => (
                <SelectItem key={g.id} value={String(g.id)} text={g.label} />
              ))}
            </Select>
          </Column>

          <Column lg={5} md={4} sm={4}>
            <Select
              id="vec-species"
              labelText={intl.formatMessage({
                id: "vector.species",
                defaultMessage: "Species",
              })}
              value={envFields.vecSpeciesId || ""}
              onChange={handleSpeciesChange}
              disabled={isReadOnly || !envFields.vecOrganismGroupId}
            >
              <SelectItem value="" text="" />
              {speciesList.map((s) => (
                <SelectItem
                  key={s.id}
                  value={String(s.id)}
                  text={`${s.genus}${s.species ? " " + s.species : ""}`}
                />
              ))}
            </Select>
          </Column>

          <Column lg={6} md={4} sm={4}>
            <Select
              id="vec-trap"
              labelText={intl.formatMessage({
                id: "vector.trapType",
                defaultMessage: "Trap Type",
              })}
              value={envFields.vecTrapTypeId || ""}
              onChange={(e) => updateEnvField("vecTrapTypeId", e.target.value)}
              disabled={isReadOnly || !envFields.vecOrganismGroupId}
            >
              <SelectItem value="" text="" />
              {trapTypes.map((t) => (
                <SelectItem key={t.id} value={String(t.id)} text={t.name} />
              ))}
            </Select>
          </Column>
        </Grid>

        {/* Row 2: Lifecycle Stage + Pathogens of Interest */}
        <Grid narrow>
          <Column lg={5} md={4} sm={4}>
            <Select
              id="vec-lifecycle"
              labelText={intl.formatMessage({
                id: "vector.lifecycleStage",
                defaultMessage: "Lifecycle Stage",
              })}
              value={envFields.vecLifecycleStage || ""}
              onChange={(e) =>
                updateEnvField("vecLifecycleStage", e.target.value)
              }
              disabled={isReadOnly || lifecycleOptions.length === 0}
            >
              <SelectItem value="" text="" />
              {lifecycleOptions.map((stage) => (
                <SelectItem key={stage} value={stage} text={stage} />
              ))}
            </Select>
          </Column>

          <Column lg={11} md={4} sm={4}>
            <p
              style={{
                fontSize: "0.75rem",
                color: "#525252",
                marginBottom: "0.25rem",
              }}
            >
              <FormattedMessage
                id="vector.pathogensOfInterest"
                defaultMessage="Pathogens of Interest"
              />
            </p>
            <div
              style={{
                display: "flex",
                flexWrap: "wrap",
                gap: "0.25rem",
                minHeight: "2rem",
              }}
            >
              {(selectedSpecies?.pathogensOfInterest || []).map((d) => (
                <Tag key={d.id} type="warm-gray" size="sm">
                  {d.dictEntry}
                </Tag>
              ))}
            </div>
          </Column>
        </Grid>

        {/* Row 3: Collection Site */}
        <Grid narrow>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="vec-collection-site-name"
              labelText={intl.formatMessage({
                id: "vector.collectionSiteName",
                defaultMessage: "Collection Site Name",
              })}
              value={envFields.vecCollectionSiteName || ""}
              onChange={(e) =>
                updateEnvField("vecCollectionSiteName", e.target.value)
              }
              readOnly={isReadOnly}
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="vec-gps-lat"
              labelText={intl.formatMessage({
                id: "vector.gpsLatitude",
                defaultMessage: "GPS Latitude",
              })}
              value={envFields.vecGpsLatitude || ""}
              onChange={(e) => updateEnvField("vecGpsLatitude", e.target.value)}
              readOnly={isReadOnly}
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="vec-gps-lon"
              labelText={intl.formatMessage({
                id: "vector.gpsLongitude",
                defaultMessage: "GPS Longitude",
              })}
              value={envFields.vecGpsLongitude || ""}
              onChange={(e) =>
                updateEnvField("vecGpsLongitude", e.target.value)
              }
              readOnly={isReadOnly}
            />
          </Column>
        </Grid>

        {/* Row 4: Pooling */}
        <Grid narrow>
          <Column lg={4} md={2} sm={2}>
            <Select
              id="vec-pooling-method"
              labelText={intl.formatMessage({
                id: "vector.poolingMethod",
                defaultMessage: "Pooling Method",
              })}
              value={envFields.vecPoolingMethod || ""}
              onChange={(e) =>
                updateEnvField("vecPoolingMethod", e.target.value)
              }
              disabled={isReadOnly}
            >
              <SelectItem value="" text="" />
              <SelectItem
                value="homogeneous"
                text={intl.formatMessage({
                  id: "vector.homogeneous",
                  defaultMessage: "Homogeneous",
                })}
              />
              <SelectItem
                value="random"
                text={intl.formatMessage({
                  id: "vector.random",
                  defaultMessage: "Random",
                })}
              />
            </Select>
          </Column>

          <Column lg={3} md={2} sm={2}>
            <TextInput
              id="vec-pool-count"
              type="number"
              min="1"
              labelText={intl.formatMessage({
                id: "vector.poolCount",
                defaultMessage: "Number of Pools",
              })}
              value={envFields.vecPoolCount || ""}
              onChange={(e) => updateEnvField("vecPoolCount", e.target.value)}
              readOnly={isReadOnly}
            />
          </Column>

          <Column lg={3} md={2} sm={2}>
            <TextInput
              id="vec-samples-per-pool"
              type="number"
              min="1"
              labelText={intl.formatMessage({
                id: "vector.samplesPerPool",
                defaultMessage: "Specimens per Pool",
              })}
              value={envFields.vecSamplesPerPool || ""}
              onChange={(e) =>
                updateEnvField("vecSamplesPerPool", e.target.value)
              }
              readOnly={isReadOnly}
            />
          </Column>

          {totalSpecimens !== null && (
            <Column lg={6} md={4} sm={4} style={{ alignSelf: "flex-end" }}>
              <p style={{ fontSize: "0.875rem", color: "#525252" }}>
                <FormattedMessage
                  id="vector.totalSpecimens"
                  defaultMessage="Total Specimens"
                />
                {": "}
                <strong>{totalSpecimens}</strong>
              </p>
            </Column>
          )}
        </Grid>
      </Stack>
    </Tile>
  );
}

export default VectorSection;
