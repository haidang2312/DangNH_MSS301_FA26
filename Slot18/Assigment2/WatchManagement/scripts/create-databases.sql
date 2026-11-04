-- TODO-2.1: create only missing databases; preserve existing data and logins.
IF DB_ID('DBAccount') IS NULL CREATE DATABASE DBAccount;
IF DB_ID('DBBrand') IS NULL CREATE DATABASE DBBrand;
IF DB_ID('DBWatch') IS NULL CREATE DATABASE DBWatch;
GO
