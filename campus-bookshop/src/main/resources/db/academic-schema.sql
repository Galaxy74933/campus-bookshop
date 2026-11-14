-- 专业、课程与图书关联：只新增表，不修改或清空已有数据，可重复执行。
CREATE TABLE IF NOT EXISTS t_major (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    UNIQUE KEY uk_major_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    UNIQUE KEY uk_course_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_major_course (
    major_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    PRIMARY KEY (major_id, course_id),
    CONSTRAINT fk_mc_major FOREIGN KEY (major_id) REFERENCES t_major(id),
    CONSTRAINT fk_mc_course FOREIGN KEY (course_id) REFERENCES t_course(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_book_course (
    book_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, course_id),
    CONSTRAINT fk_bc_book FOREIGN KEY (book_id) REFERENCES t_book(id) ON DELETE CASCADE,
    CONSTRAINT fk_bc_course FOREIGN KEY (course_id) REFERENCES t_course(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
