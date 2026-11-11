-- =====================================================================================
-- Initialisation PostgreSQL pour CSME ERP (développement local, CI, première installation).
-- À exécuter une seule fois avec un superutilisateur.
--
--   csme_owner : propriétaire du schéma, utilisé UNIQUEMENT par Flyway pour les migrations.
--   csme_app   : rôle d'exécution de l'API. Pas superutilisateur, pas propriétaire :
--                PostgreSQL Row Level Security s'applique donc à toutes ses requêtes.
--
-- En production, remplacer les mots de passe par des secrets (gestionnaire de secrets du cloud).
-- =====================================================================================

create role csme_owner login password 'csme_owner_dev' nosuperuser nocreaterole nocreatedb;
create role csme_app login password 'csme_app_dev' nosuperuser nocreaterole nocreatedb noinherit;

create database csme owner csme_owner;
create database csme_test owner csme_owner;

\connect csme
revoke all on schema public from public;
alter schema public owner to csme_owner;

\connect csme_test
revoke all on schema public from public;
alter schema public owner to csme_owner;
