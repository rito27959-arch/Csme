-- =====================================================================================
-- CSME ERP — V2 : droits du rôle d'exécution de l'API.
--
-- Les migrations s'exécutent avec le rôle propriétaire du schéma ; l'API s'exécute avec
-- un rôle distinct, sans privilège de superutilisateur (sinon RLS serait contournée).
-- Le nom du rôle est fourni par le placeholder Flyway ${app_role}.
-- =====================================================================================

grant usage on schema public to ${app_role};

grant select, insert, update, delete on
    companies, agencies, users, roles, role_permissions, user_roles, user_agencies, refresh_tokens
    to ${app_role};

-- Audit : l'application peut lire et ajouter, jamais modifier ni supprimer.
grant select, insert on audit_logs to ${app_role};
revoke update, delete, truncate on audit_logs from ${app_role};

grant execute on function csme_current_company() to ${app_role};
grant execute on function csme_is_system() to ${app_role};
