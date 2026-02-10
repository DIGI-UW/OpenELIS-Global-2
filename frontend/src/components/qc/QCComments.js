import React, { useState } from "react";
import {
  TextArea,
  Button,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
} from "@carbon/react";
import { useIntl } from "react-intl";

const QCComments = ({ comments, onAddComment }) => {
  const intl = useIntl();
  const [newComment, setNewComment] = useState("");

  const handleAdd = () => {
    if (newComment.trim() && onAddComment) {
      onAddComment(newComment.trim());
      setNewComment("");
    }
  };

  return (
    <div>
      <h4>{intl.formatMessage({ id: "qc.comments.title" })}</h4>

      <div style={{ marginBottom: "1rem" }}>
        <TextArea
          id="qc-comment-input"
          labelText=""
          placeholder={intl.formatMessage({ id: "qc.comments.placeholder" })}
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
        />
        <Button
          size="sm"
          onClick={handleAdd}
          disabled={!newComment.trim()}
          style={{ marginTop: "0.5rem" }}
        >
          {intl.formatMessage({ id: "qc.comments.add" })}
        </Button>
      </div>

      {!comments || comments.length === 0 ? (
        <p>{intl.formatMessage({ id: "qc.comments.empty" })}</p>
      ) : (
        <StructuredListWrapper>
          <StructuredListHead>
            <StructuredListRow head>
              <StructuredListCell head>Comment</StructuredListCell>
              <StructuredListCell head>Date</StructuredListCell>
              <StructuredListCell head>User</StructuredListCell>
            </StructuredListRow>
          </StructuredListHead>
          <StructuredListBody>
            {comments.map((c, idx) => (
              <StructuredListRow key={idx}>
                <StructuredListCell>{c.text}</StructuredListCell>
                <StructuredListCell>{c.date || ""}</StructuredListCell>
                <StructuredListCell>{c.user || ""}</StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      )}
    </div>
  );
};

export default QCComments;
