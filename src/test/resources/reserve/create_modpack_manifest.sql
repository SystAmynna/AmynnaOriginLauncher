
CREATE TABLE IF NOT EXISTS Files (
    path VARCHAR(200) PRIMARY KEY UNIQUE NOT NULL,
    url VARCHAR(500),
    hash VARCHAR(200),
    size LONG,
    name VARCHAR(50),
    description TEXT
);

CREATE TABLE IF NOT EXISTS Dependencies (
    file VARCHAR(200) NOT NULL REFERENCES Files(path),
    dependency VARCHAR(200) NOT NULL REFERENCES Files(path)
);


CREATE TABLE IF NOT EXISTS ToInstall (
    file VARCHAR(200) PRIMARY KEY NOT NULL REFERENCES Files(path),
    optional BOOLEAN NOT NULL,
    admin_required BOOLEAN NOT NULL
);
