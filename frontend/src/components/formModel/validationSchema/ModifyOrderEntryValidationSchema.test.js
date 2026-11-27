import createModifyOrderEntryValidationSchema from "./ModifyOrderEntryValidationSchema";

// OGC-1366 walk: Modify Order demanded the requester's names on every order,
// with no * on the fields, while order entry asks for them only when
// REQUESTER_REQUIRED is on. An order saved without a requester could not be
// edited at all.
const orderWithoutRequester = () => ({
  sampleOrderItems: {
    labNo: "DEV01260000000000413",
    referringSiteId: "42",
    providerFirstName: "",
    providerLastName: "",
  },
});

const errorsFor = async (configurationProperties) => {
  try {
    await createModifyOrderEntryValidationSchema(
      configurationProperties,
    ).validate(orderWithoutRequester(), { abortEarly: false });
    return [];
  } catch (e) {
    return e.errors;
  }
};

describe("Modify Order validation — requester names (OGC-1366)", () => {
  test("are optional when REQUESTER_REQUIRED is off", async () => {
    expect(await errorsFor({ REQUESTER_REQUIRED: "false" })).toEqual([]);
  });

  test("are optional when the property is not published", async () => {
    expect(await errorsFor(undefined)).toEqual([]);
  });

  test("are required when REQUESTER_REQUIRED is on", async () => {
    expect(await errorsFor({ REQUESTER_REQUIRED: "true" })).toEqual(
      expect.arrayContaining([
        "Requester Last Name is required",
        "Requester First Name is required",
      ]),
    );
  });

  test("the referring site stays required either way", async () => {
    const order = orderWithoutRequester();
    delete order.sampleOrderItems.referringSiteId;
    await expect(
      createModifyOrderEntryValidationSchema({
        REQUESTER_REQUIRED: "false",
      }).validate(order),
    ).rejects.toThrow("Referring Site is required");
  });
});
