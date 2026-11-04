WITH RECURSIVE codigos(numero) AS (
    SELECT 0
    UNION ALL
    SELECT numero + 1 FROM codigos WHERE numero < 9999
),
faltantes AS (
    SELECT e.id, row_number() OVER (ORDER BY e.id) AS posicion
    FROM estudiantes e LEFT JOIN alumnos a ON a.estudiante_id = e.id
    WHERE a.estudiante_id IS NULL
),
libres AS (
    SELECT printf('%04d', numero) AS codigo, row_number() OVER (ORDER BY numero) AS posicion
    FROM codigos
    WHERE NOT EXISTS (SELECT 1 FROM alumnos WHERE codigo = printf('%04d', numero))
)
INSERT INTO alumnos (estudiante_id, codigo)
SELECT f.id, l.codigo FROM faltantes f LEFT JOIN libres l USING (posicion);
