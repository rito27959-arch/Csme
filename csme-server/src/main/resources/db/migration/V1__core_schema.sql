-- =====================================================================================
-- CSME ERP — V1 : socle multi-entreprises, utilisateurs, rôles, jetons, audit.
--
-- Principe d'isolation : chaque table métier porte company_id et est protégée par
-- PostgreSQL Row Level Security (RLS). L'API positionne, pour chaque connexion :
--   app.company_id  = identifiant de l'entreprise de l'utilisateur authentifié
--   app.bypass_rls  = 'on' uniquement pour les opérations plateforme explicitement
--                     élevées dans le code (connexion, super administrateur).
-- Même si un bug applicatif oubliait un filtre, la base refuserait de renvoyer ou
-- d'écrire les lignes d'une autre entreprise.
-- =====================================================================================

create or replace function csme_current_company() returns uuid
    language sql stable as
$$ select nullif(current_setting('app.company_id', true), '')::uuid $$;

create or replace function csme_is_system() returns boolean
    language sql stable as
$$ select coalesce(current_setting('app.bypass_rls', true), '') = 'on' $$;

-- -------------------------------------------------------------------------------------
-- Entreprises
-- -------------------------------------------------------------------------------------
create table companies
(
    id         uuid primary key     default gen_random_uuid(),
    code       varchar(32)  not null,
    name       varchar(160) not null,
    legal_name varchar(200),
    currency   varchar(3)   not null,
    country    varchar(2)   not null,
    active     boolean      not null default true,
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now(),
    version    bigint       not null default 0,
    constraint uq_companies_code unique (code),
    constraint ck_companies_code check (code ~ '^[A-Z0-9][A-Z0-9_-]{1,31}$'),
    constraint ck_companies_currency check (currency ~ '^[A-Z]{3}$'),
    constraint ck_companies_country check (country ~ '^[A-Z]{2}$')
);

-- -------------------------------------------------------------------------------------
-- Agences
-- -------------------------------------------------------------------------------------
create table agencies
(
    id         uuid primary key     default gen_random_uuid(),
    company_id uuid         not null references companies (id),
    code       varchar(32)  not null,
    name       varchar(160) not null,
    city       varchar(120),
    phone      varchar(40),
    active     boolean      not null default true,
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now(),
    version    bigint       not null default 0,
    constraint uq_agencies_company_code unique (company_id, code)
);
create index ix_agencies_company on agencies (company_id);

-- -------------------------------------------------------------------------------------
-- Utilisateurs (company_id nul = super administrateur de la plateforme)
-- -------------------------------------------------------------------------------------
create table users
(
    id                  uuid primary key      default gen_random_uuid(),
    company_id          uuid references companies (id),
    email               varchar(254) not null,
    full_name           varchar(160) not null,
    job_title           varchar(120),
    password_hash       varchar(255) not null,
    active              boolean      not null default true,
    super_admin         boolean      not null default false,
    failed_login_count  integer      not null default 0,
    locked_until        timestamptz,
    last_login_at       timestamptz,
    password_changed_at timestamptz  not null default now(),
    created_at          timestamptz  not null default now(),
    updated_at          timestamptz  not null default now(),
    version             bigint       not null default 0,
    constraint ck_users_email_lower check (email = lower(email)),
    constraint ck_users_scope check ((super_admin and company_id is null) or (not super_admin and company_id is not null))
);
create unique index uq_users_email on users (email);
create index ix_users_company on users (company_id);

-- -------------------------------------------------------------------------------------
-- Rôles et permissions (MODULE:ACTION)
-- -------------------------------------------------------------------------------------
create table roles
(
    id          uuid primary key      default gen_random_uuid(),
    company_id  uuid         not null references companies (id),
    name        varchar(80)  not null,
    description varchar(255),
    system_role boolean      not null default false,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now(),
    version     bigint       not null default 0,
    constraint uq_roles_company_name unique (company_id, name)
);
create index ix_roles_company on roles (company_id);

create table role_permissions
(
    role_id uuid        not null references roles (id) on delete cascade,
    module  varchar(40) not null,
    action  varchar(20) not null,
    primary key (role_id, module, action)
);

create table user_roles
(
    user_id uuid not null references users (id) on delete cascade,
    role_id uuid not null references roles (id) on delete cascade,
    primary key (user_id, role_id)
);
create index ix_user_roles_role on user_roles (role_id);

create table user_agencies
(
    user_id   uuid not null references users (id) on delete cascade,
    agency_id uuid not null references agencies (id) on delete cascade,
    primary key (user_id, agency_id)
);

-- -------------------------------------------------------------------------------------
-- Jetons de rafraîchissement (stockés hachés, rotation + détection de réutilisation)
-- -------------------------------------------------------------------------------------
create table refresh_tokens
(
    id          uuid primary key     default gen_random_uuid(),
    user_id     uuid        not null references users (id) on delete cascade,
    company_id  uuid references companies (id),
    family_id   uuid        not null,
    token_hash  varchar(64) not null,
    expires_at  timestamptz not null,
    revoked_at  timestamptz,
    replaced_by uuid,
    user_agent  varchar(255),
    ip_address  varchar(64),
    created_at  timestamptz not null default now(),
    constraint uq_refresh_tokens_hash unique (token_hash)
);
create index ix_refresh_tokens_user on refresh_tokens (user_id);
create index ix_refresh_tokens_family on refresh_tokens (family_id);

-- -------------------------------------------------------------------------------------
-- Journal d'audit (insertion seule pour l'application, voir V2)
-- -------------------------------------------------------------------------------------
create table audit_logs
(
    id          bigint generated always as identity primary key,
    company_id  uuid references companies (id),
    actor_id    uuid,
    actor_email varchar(254),
    action      varchar(64) not null,
    entity_type varchar(64),
    entity_id   varchar(64),
    outcome     varchar(16) not null,
    details     text,
    ip_address  varchar(64),
    occurred_at timestamptz not null default now(),
    constraint ck_audit_outcome check (outcome in ('SUCCESS', 'FAILURE', 'DENIED'))
);
create index ix_audit_company_time on audit_logs (company_id, occurred_at desc);
create index ix_audit_action_time on audit_logs (action, occurred_at desc);

-- -------------------------------------------------------------------------------------
-- Row Level Security
-- -------------------------------------------------------------------------------------
alter table companies enable row level security;
alter table companies force row level security;
create policy tenant_isolation on companies
    using (csme_is_system() or id = csme_current_company())
    with check (csme_is_system());

alter table agencies enable row level security;
alter table agencies force row level security;
create policy tenant_isolation on agencies
    using (csme_is_system() or company_id = csme_current_company())
    with check (csme_is_system() or company_id = csme_current_company());

alter table users enable row level security;
alter table users force row level security;
create policy tenant_isolation on users
    using (csme_is_system() or company_id = csme_current_company())
    with check (csme_is_system() or company_id = csme_current_company());

alter table roles enable row level security;
alter table roles force row level security;
create policy tenant_isolation on roles
    using (csme_is_system() or company_id = csme_current_company())
    with check (csme_is_system() or company_id = csme_current_company());

-- Tables de liaison : visibles seulement si les deux extrémités le sont (sous-requêtes elles-mêmes filtrées).
alter table role_permissions enable row level security;
alter table role_permissions force row level security;
create policy tenant_isolation on role_permissions
    using (exists (select 1 from roles r where r.id = role_id))
    with check (exists (select 1 from roles r where r.id = role_id));

alter table user_roles enable row level security;
alter table user_roles force row level security;
create policy tenant_isolation on user_roles
    using (exists (select 1 from users u where u.id = user_id) and exists (select 1 from roles r where r.id = role_id))
    with check (exists (select 1 from users u where u.id = user_id) and exists (select 1 from roles r where r.id = role_id));

alter table user_agencies enable row level security;
alter table user_agencies force row level security;
create policy tenant_isolation on user_agencies
    using (exists (select 1 from users u where u.id = user_id) and exists (select 1 from agencies a where a.id = agency_id))
    with check (exists (select 1 from users u where u.id = user_id) and exists (select 1 from agencies a where a.id = agency_id));

-- Jetons : connexion/rafraîchissement en contexte système ; une entreprise peut révoquer
-- les jetons de ses propres utilisateurs (désactivation, réinitialisation de mot de passe).
alter table refresh_tokens enable row level security;
alter table refresh_tokens force row level security;
create policy tenant_isolation on refresh_tokens
    using (csme_is_system() or company_id = csme_current_company())
    with check (csme_is_system() or company_id = csme_current_company());

alter table audit_logs enable row level security;
alter table audit_logs force row level security;
create policy tenant_read on audit_logs for select
    using (csme_is_system() or company_id = csme_current_company());
create policy tenant_insert on audit_logs for insert
    with check (csme_is_system() or company_id = csme_current_company());
