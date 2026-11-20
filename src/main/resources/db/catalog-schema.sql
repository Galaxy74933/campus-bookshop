-- Non-destructive upgrade: preserve existing books, orders and retired academic tables.
CREATE TABLE IF NOT EXISTS t_import_batch (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 owner_id BIGINT,
 filename VARCHAR(255) NOT NULL,
 resource_count INT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 undone_at DATETIME,
 INDEX idx_import_owner(owner_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS t_import_batch_book (
 batch_id BIGINT NOT NULL,
 book_id BIGINT NOT NULL,
 PRIMARY KEY(batch_id,book_id),
 CONSTRAINT fk_import_batch FOREIGN KEY(batch_id) REFERENCES t_import_batch(id),
 CONSTRAINT fk_import_book FOREIGN KEY(book_id) REFERENCES t_book(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS t_category (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 UNIQUE KEY uk_category_name(name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS t_book_category (
 book_id BIGINT PRIMARY KEY,
 category_id BIGINT NOT NULL,
 CONSTRAINT fk_bcat_book FOREIGN KEY(book_id) REFERENCES t_book(id) ON DELETE CASCADE,
 CONSTRAINT fk_bcat_category FOREIGN KEY(category_id) REFERENCES t_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS t_reading_list (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 owner_id BIGINT NOT NULL,
 name VARCHAR(100) NOT NULL,
 description VARCHAR(500) NOT NULL DEFAULT '',
 public_list BOOLEAN NOT NULL DEFAULT FALSE,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_reading_owner_name(owner_id, name),
 CONSTRAINT fk_reading_owner FOREIGN KEY(owner_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS t_reading_list_book (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 list_id BIGINT NOT NULL,
 book_id BIGINT NOT NULL,
 UNIQUE KEY uk_reading_book(list_id, book_id),
 CONSTRAINT fk_reading_item_list FOREIGN KEY(list_id) REFERENCES t_reading_list(id) ON DELETE CASCADE,
 CONSTRAINT fk_reading_item_book FOREIGN KEY(book_id) REFERENCES t_book(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS t_app_migration (
 name VARCHAR(100) PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Resource extension: preserve legacy book rows and their single primary category.
CREATE TABLE IF NOT EXISTS t_resource_info (
 book_id BIGINT PRIMARY KEY,
 resource_type VARCHAR(30) NOT NULL DEFAULT '图书',
 language VARCHAR(30) NOT NULL DEFAULT '',
 author_unit VARCHAR(200) NOT NULL DEFAULT '',
 publisher VARCHAR(200) NOT NULL DEFAULT '',
 publish_date DATE,
 identifier VARCHAR(200) NOT NULL DEFAULT '',
 description VARCHAR(2000) NOT NULL DEFAULT '',
 CONSTRAINT fk_resource_book FOREIGN KEY(book_id) REFERENCES t_book(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS t_resource_category (
 book_id BIGINT NOT NULL,
 category_id BIGINT NOT NULL,
 PRIMARY KEY(book_id,category_id),
 CONSTRAINT fk_resource_category_book FOREIGN KEY(book_id) REFERENCES t_book(id) ON DELETE CASCADE,
 CONSTRAINT fk_resource_category_category FOREIGN KEY(category_id) REFERENCES t_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
