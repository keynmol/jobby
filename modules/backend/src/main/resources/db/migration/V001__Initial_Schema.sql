CREATE TABLE users(
  user_id uuid PRIMARY KEY,
  login character varying(50) not null,
  salted_hash character(81) not null -- 64 for the hash + 16 for the salt + ':',
);
-----
CREATE UNIQUE INDEX users_login_idx ON users (LOWER(login));

CREATE TABLE companies(
  company_id uuid PRIMARY KEY,
  owner_id uuid not null,
  name character varying(128) not null,
  description text,
  url character varying(512) not null,
  CONSTRAINT fk_owner FOREIGN KEY(owner_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX company_name_idx ON companies (LOWER(name));
-----
CREATE TABLE jobs(
  job_id uuid primary key,
  company_id uuid not null,
  job_title character varying(256) not null,
  job_description text not null,
  job_url character varying(512) not null,
  min_salary int4 not null,
  max_salary int4 not null,
  CONSTRAINT fk_company FOREIGN KEY(company_id) REFERENCES companies(company_id) ON DELETE CASCADE
);
-----
CREATE TABLE profiles(
  profile_id uuid primary key,
  user_id uuid not null,
  profile_title character varying(256) not null,
  profile_pitch text,
  CONSTRAINT fk_owner FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
-----
CREATE TABLE applications(
  job_id uuid not null,
  profile_id uuid not null,
  CONSTRAINT fk_job FOREIGN KEY(job_id) REFERENCES jobs(job_id) ON DELETE CASCADE,
  CONSTRAINT fk_profile FOREIGN KEY(profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE
);
