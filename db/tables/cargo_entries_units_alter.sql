ALTER TABLE cargo_entries_units
ADD CONSTRAINT `cargo_entries_units_fk_basis` FOREIGN KEY (`basis_id`) REFERENCES `bases` (`id`),
ADD CONSTRAINT `cargo_entries_units_fk_schiff` FOREIGN KEY (`schiff_id`) REFERENCES `ships` (`id`);