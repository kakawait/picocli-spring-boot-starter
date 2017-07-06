CREATE TABLE person (
  id BIGINT AUTO_INCREMENT,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  PRIMARY KEY(id)
);

insert into person (first_name, last_name) values ('Thibaud', 'Lepretre');
