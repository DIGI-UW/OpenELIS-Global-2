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

  describe("moveSampleItem", () => {
    // NEW FLEXIBLE ASSIGNMENT ARCHITECTURE: Use locationId + locationType + positionCoordinate
    // Storage tracking operates at SampleItem level (physical specimens), not Sample level (orders)
    const movementData = {
      sampleItemId: "SI-2025-001",
      locationId: "123",
      locationType: "device",
      positionCoordinate: null,
      reason: "Test move",
    };

    /**
     * Test: moveSampleItem successfully moves sample item when API returns success response
     */
    test("testMoveSampleItem_Success_ReturnsResponse", async () => {
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
        moveResult = await result.current.moveSampleItem(movementData);
      });

      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
        "/rest/storage/sample-items/move",
        JSON.stringify(movementData),
        expect.any(Function),
      );

      expect(moveResult).toEqual(mockResponse);
      expect(result.current.error).toBeNull();
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: moveSampleItem handles error response with message field
     */
    test("testMoveSampleItem_ErrorWithMessage_RejectsWithError", async () => {
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
        await expect(
          result.current.moveSampleItem(movementData),
        ).rejects.toThrow("Target position is already occupied");
      });

      expect(result.current.error).toBe("Target position is already occupied");
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: moveSampleItem handles error response with error field
     */
    test("testMoveSampleItem_ErrorWithErrorField_RejectsWithError", async () => {
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
          result.current.moveSampleItem(movementData),
        ).rejects.toThrow("Sample not found");
      });

      expect(result.current.error).toBe("Sample not found");
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: moveSampleItem handles unexpected response format
     */
    test("testMoveSampleItem_UnexpectedResponse_RejectsWithError", async () => {
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
          result.current.moveSampleItem(movementData),
        ).rejects.toThrow();
      });

      // The error message will be "[object Object]" because response.toString() returns that for objects
      expect(result.current.error).toBeTruthy();
      expect(result.current.error).toMatch(
        /Unexpected response format|\[object Object\]/,
      );
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: moveSampleItem sets isSubmitting state correctly
     */
    test("testMoveSampleItem_SetsIsSubmittingState", async () => {
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
        result.current.moveSampleItem(movementData);
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

  describe("assignSampleItem", () => {
    // NEW FLEXIBLE ASSIGNMENT ARCHITECTURE: Use locationId + locationType + positionCoordinate
    // Storage tracking operates at SampleItem level (physical specimens), not Sample level (orders)
    const assignmentData = {
      sampleItemId: "SI-2025-001",
      locationId: "123",
      locationType: "device",
      positionCoordinate: null,
      notes: "Test assignment",
    };

    /**
     * Test: assignSampleItem successfully assigns sample item when API returns success response
     */
    test("testAssignSampleItem_Success_ReturnsResponse", async () => {
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
        assignResult = await result.current.assignSampleItem(assignmentData);
      });

      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
        "/rest/storage/sample-items/assign",
        JSON.stringify(assignmentData),
        expect.any(Function),
      );

      expect(assignResult).toEqual(mockResponse);
      expect(result.current.error).toBeNull();
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: assignSampleItem handles error response with message field
     */
    test("testAssignSampleItem_ErrorWithMessage_RejectsWithError", async () => {
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
          result.current.assignSampleItem(assignmentData),
        ).rejects.toThrow("Position is already occupied");
      });

      expect(result.current.error).toBe("Position is already occupied");
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: assignSampleItem handles error response with error field
     */
    test("testAssignSampleItem_ErrorWithErrorField_RejectsWithError", async () => {
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
          result.current.assignSampleItem(assignmentData),
        ).rejects.toThrow("Sample not found");
      });

      expect(result.current.error).toBe("Sample not found");
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: assignSampleItem handles unexpected response format
     */
    test("testAssignSampleItem_UnexpectedResponse_RejectsWithError", async () => {
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
          result.current.assignSampleItem(assignmentData),
        ).rejects.toThrow();
      });

      // The error message will be "[object Object]" because response.toString() returns that for objects
      expect(result.current.error).toBeTruthy();
      expect(result.current.error).toMatch(
        /Unexpected response format|\[object Object\]/,
      );
      expect(result.current.isSubmitting).toBe(false);
    });

    /**
     * Test: assignSampleItem accepts response with only hierarchicalPath (no assignmentId)
     */
    test("testAssignSampleItem_SuccessWithOnlyHierarchicalPath", async () => {
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
        assignResult = await result.current.assignSampleItem(assignmentData);
      });

      expect(assignResult).toEqual(mockResponse);
      expect(result.current.error).toBeNull();
    });
  });
});
