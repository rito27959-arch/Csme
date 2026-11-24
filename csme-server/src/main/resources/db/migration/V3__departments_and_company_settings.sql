-- =====================================================================================
-- CSME ERP — V3 : départements, informations générales de l'entreprise,
-- affectation des utilisateurs à un département.
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- Informations générales de l'entreprise (modifiables par l'entreprise elle-même)
-- -------------------------------------------------------------------------------------
alter table companies
    add column address        varchar(255),
    add column city           varchar(120),
    add column phone          varchar(40),
    add column email          varchar(254),
    add column website        varchar(255),
    add column tax_id         varchar(64),
    add column trade_register varchar(64);

-- Jusqu'ici, seule la plateforme pouvait écrire dans companies. Une entreprise peut désormais
-- lire et modifier SA ligne ; la création et la suppression restent réservées à la plateforme.
drop policy tenant_isolation on companies;
create policy company_select on companies for select
    using (csme_is_system() or id = csme_current_company());
create policy company_update on companies for update
    using (csme_is_system() or id = csme_current_company())
    with check (csme_is_system() or id = csme_current_company());
create policy company_insert on companies for insert
    with check (csme_is_system());
create policy company_delete on companies for delete
    using (csme_is_system());

-- -------------------------------------------------------------------------------------
-- Départements (une agence possède plusieurs départements)
-- -------------------------------------------------------------------------------------
create table departments
(
    id          uuid primary key,
    company_id  uuid         not null references companies (id),
    agency_id   uuid         not null references agencies (id),
    code        varchar(32)  not null,
    name        varchar(160) not null,
    description varchar(255),
    active      boolean      not null default true,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now(),
    version     bigint       not null default 0,
    constraint uq_departments_company_code unique (company_id, code)
);
create index ix_departments_company on departments (company_id);
create index ix_departments_agency on departments (agency_id);

alter table departments enable row level security;
alter table departments force row level security;
create policy tenant_isolation on departments
    using (csme_is_system() or company_id = csme_current_company())
    with check (csme_is_system() or company_id = csme_current_company());

-- Cohérence : l'agence d'un département appartient à la même entreprise.
create or replace function csme_check_department_agency() returns trigger
    language plpgsql as
$$
begin
    if not exists (select 1 from agencies a where a.id = new.agency_id and a.company_id = new.company_id) then
        raise exception 'L''agence % n''appartient pas à l''entreprise %', new.agency_id, new.company_id
            using errcode = '23514';
    end if;
    return new;
end
$$;
create trigger trg_departments_agency
    before insert or update of agency_id, company_id on departments
    for each row execute function csme_check_department_agency();

-- -------------------------------------------------------------------------------------
-- Affectation d'un utilisateur à un département
-- -------------------------------------------------------------------------------------
alter table users add column department_id uuid references departments (id);
create index ix_users_department on users (department_id);

create or replace function csme_check_user_department() returns trigger
    language plpgsql as
$$
begin
    if new.department_id is not null and not exists (
        select 1 from departments d where d.id = new.department_id and d.company_id = new.company_id) then
        raise exception 'Le département % n''appartient pas à l''entreprise de l''utilisateur', new.department_id
            using errcode = '23514';
    end if;
    return new;
end
$$;
create trigger trg_users_department
    before insert or update of department_id on users
    for each row execute function csme_check_user_department();

grant select, insert, update, delete on departments to ${app_role};

-- -------------------------------------------------------------------------------------
-- Nouveau module DEPARTMENTS : les rôles existants reçoivent sur les départements
-- les mêmes actions que celles qu'ils ont déjà sur les agences.
-- -------------------------------------------------------------------------------------
insert into role_permissions (role_id, module, action)
select role_id, 'DEPARTMENTS', action
from role_permissions
where module = 'AGENCIES'
on conflict do nothing;
