-- A second, disposable database used only by `mvn -Pcodegen generate-sources`:
-- Flyway cleans and re-applies every migration into it, and jOOQ generates from the
-- resulting catalog. Never used by the running application.
CREATE DATABASE tabletop_codegen OWNER tabletop;
