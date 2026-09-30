-- Apply once before deploying AI service; preserves existing rows.
ALTER TABLE document_chunk ADD COLUMN metadata_json MEDIUMTEXT NULL COMMENT '课程版本及构建元数据';
CREATE INDEX idx_chunk_vector_active ON document_chunk (vector_id, is_delete);
