import { renderHook, act } from "@testing-library/react-hooks";
import { useSampleStorage } from "./useSampleStorage";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";

// Mock the API utilities
jest.mock("../../utils/Utils", () => ({
  postToOpenElisServerJsonResponse: jest.fn(),
}));

describe("useSampleStorage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("moveSample", () => {
    // NEW FLEXIBLE ASSIGNMENT ARCHITECTURE: Use locationId + locationType + positionCoordinate
    const movementData = {
      sampleId: "S-2025-001",
      locationId: "123",
      locationType: "device",
      positionCoordinate: null,
      reason: "Test move",
    };

    /**
     * Test: moveSample successfully moves sample when API returns success response
     */
    test("testMoveSample_Success_ReturnsResponse", async () => {
      const mockResponse = {
        movementId: "movement-123",
        previousLocation: "Main Laboratory > Freezer Unit 1",
        newLocation: "Main Laboratory > Refrigerator 2",
        movedDate: "2025-01-15T10:30:00",
      };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, callback) => {
          callback(mockResponse);
        },
      );

      const { result } = renderHook(() => useSampleStorage());

      let moveResult;
      await act(async () => {
        moveResult = await result.current.moveSample(movementData);
      });

      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
        "/rest/storage/samples/move",
        JSON.stringify(movementData),
        expect.any(Function),
      );

      expect(moveResult).toEqual(mockResponse);
      expect(result.current.error).toBeNull();
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: moveSample handles error response with message field
     */
    test("testMoveSample_ErrorWithMessage_RejectsWithError", async () => {
      const mockErrorResponse = {
        message: "Target position is already occupied",
      };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, callback) => {
          callback(mockErrorResponse);
        },
      );

      const { result } = renderHook(() => useSampleStorage());

      await act(async () => {
        await expect(result.current.moveSample(movementData)).rejects.toThrow(
          "Target position is already occupied",
        );
      });

      expect(result.current.error).toBe("Target position is already occupied");
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: moveSample handles error response with error field
     */
    test("testMoveSample_ErrorWithErrorField_RejectsWithError", async () => {
      const mockErrorResponse = {
        error: "Sample not found",
      };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, callback) => {
          callback(mockErrorResponse);
        },
      );

      const { result } = renderHook(() => useSampleStorage());

      await act(async () => {
        await expect(result.current.moveSample(movementData)).rejects.toThrow(
          "Sample not found",
        );
      });

      expect(result.current.error).toBe("Sample not found");
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: moveSample handles unexpected response format
     */
    test("testMoveSample_UnexpectedResponse_RejectsWithError", async () => {
      const mockUnexpectedResponse = {
        someField: "unexpected",
      };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, callback) => {
          callback(mockUnexpectedResponse);
        },
      );

      const { result } = renderHook(() => useSampleStorage());

      await act(async () => {
        await expect(result.current.moveSample(movementData)).rejects.toThrow(
          "Unexpected response format",
        );
      });

      expect(result.current.error).toBe("Unexpected response format");
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: moveSample sets isSubmitting state correctly
     */
    test("testMoveSample_SetsIsSubmittingState", async () => {
      const mockResponse = {
        movementId: "movement-123",
      };

      let resolveCallback;
      const promise = new Promise((resolve) => {
        resolveCallback = resolve;
      });

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, callback) => {
          promise.then(() => callback(mockResponse));
        },
      );

      const { result } = renderHook(() => useSampleStorage());

      act(() => {
        result.current.moveSample(movementData);
      });

      // Should be submitting initially
      expect(result.current.isSubmitting).toBe(true);

      await act(async () => {
        resolveCallback();
        await promise;
      });

      // Should be false after completion
      expect(result.current.isSubmitting).toBe(false);
    });
  });

  describe("assignSample", () => {
    // NEW FLEXIBLE ASSIGNMENT ARCHITECTURE: Use locationId + locationType + positionCoordinate
    const assignmentData = {
      sampleId: "S-2025-001",
      locationId: "123",
      locationType: "device",
      positionCoordinate: null,
      notes: "Test assignment",
    };

    /**
     * Test: assignSample successfully assigns sample when API returns success response
     */
    test("testAssignSample_Success_ReturnsResponse", async () => {
      const mockResponse = {
        assignmentId: "assignment-123",
        hierarchicalPath:
          "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5",
        assignedDate: "2025-01-15T10:30:00",
      };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, callback) => {
          callback(mockResponse);
        },
      );

      const { result } = renderHook(() => useSampleStorage());

      let assignResult;
      await act(async () => {
        assignResult = await result.current.assignSample(assignmentData);
      });

      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
        "/rest/storage/samples/assign",
        JSON.stringify(assignmentData),
        expect.any(Function),
      );

      expect(assignResult).toEqual(mockResponse);
      expect(result.current.error).toBeNull();
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: assignSample handles error response with message field
     */
    test("testAssignSample_ErrorWithMessage_RejectsWithError", async () => {
      const mockErrorResponse = {
        message: "Position is already occupied",
      };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, callback) => {
          callback(mockErrorResponse);
        },
      );

      const { result } = renderHook(() => useSampleStorage());

      await act(async () => {
        await expect(
          result.current.assignSample(assignmentData),
        ).rejects.toThrow("Position is already occupied");
      });

      expect(result.current.error).toBe("Position is already occupied");
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: assignSample handles error response with error field
     */
    test("testAssignSample_ErrorWithErrorField_RejectsWithError", async () => {
      const mockErrorResponse = {
        error: "Sample not found",
      };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, callback) => {
          callback(mockErrorResponse);
        },
      );

      const { result } = renderHook(() => useSampleStorage());

      await act(async () => {
        await expect(
          result.current.assignSample(assignmentData),
        ).rejects.toThrow("Sample not found");
      });

      expect(result.current.error).toBe("Sample not found");
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: assignSample handles unexpected response format
     */
    test("testAssignSample_UnexpectedResponse_RejectsWithError", async () => {
      const mockUnexpectedResponse = {
        someField: "unexpected",
      };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, callback) => {
          callback(mockUnexpectedResponse);
        },
      );

      const { result } = renderHook(() => useSampleStorage());

      await act(async () => {
        await expect(
          result.current.assignSample(assignmentData),
        ).rejects.toThrow("Unexpected response format");
      });

      expect(result.current.error).toBe("Unexpected response format");
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: assignSample accepts response with only hierarchicalPath (no assignmentId)
     */
    test("testAssignSample_SuccessWithOnlyHierarchicalPath", async () => {
      const mockResponse = {
        hierarchicalPath: "Main Laboratory > Freezer Unit 1",
      };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, callback) => {
          callback(mockResponse);
        },
      );

      const { result } = renderHook(() => useSampleStorage());

      let assignResult;
      await act(async () => {
        assignResult = await result.current.assignSample(assignmentData);
      });

      expect(assignResult).toEqual(mockResponse);
      expect(result.current.error).toBeNull();
    });
  });
});
