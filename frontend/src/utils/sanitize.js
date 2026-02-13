import DOMPurify from "dompurify";

/**
 * Sanitize HTML content for safe rendering via dangerouslySetInnerHTML.
 *
 * Only allows <br> tags (used as note separators by the backend's
 * NoteService.getNotesAsString()) and strips everything else — including
 * <script>, event-handler attributes, and any other potentially dangerous
 * markup — to prevent Stored XSS.
 *
 * @param {string} html - The raw HTML string to sanitize
 * @returns {string} Sanitized HTML safe for rendering
 */
export function sanitizeHTML(html) {
  return DOMPurify.sanitize(html || "", {
    ALLOWED_TAGS: ["br"],
    ALLOWED_ATTR: [],
  });
}
