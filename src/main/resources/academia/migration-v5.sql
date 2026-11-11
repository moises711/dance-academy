ALTER TABLE matriculas ADD COLUMN horario_id INTEGER REFERENCES horarios(id);
CREATE INDEX IF NOT EXISTS ix_matriculas_horario ON matriculas(horario_id);
