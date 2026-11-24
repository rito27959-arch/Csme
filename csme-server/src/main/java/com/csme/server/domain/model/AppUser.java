package com.csme.server.domain.model;

import com.csme.shared.security.Permission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.csme.server.tenancy.TenantContext;
import com.csme.server.tenancy.TenantScope;

/**
 * Compte utilisateur. {@code companyId} est nul uniquement pour le super administrateur de la
 * plateforme (contrainte vérifiée aussi en base). Un utilisateur n'a pas d'accès hors de son entreprise.
 */
@Entity
@Table(name = "users")
public class AppUser extends BaseEntity {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    @Column(name = "company_id", updatable = false)
    private UUID companyId;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "super_admin", nullable = false, updatable = false)
    private boolean superAdmin;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt = Instant.now();

    @jakarta.persistence.ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_agencies",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "agency_id"))
    private Set<Agency> agencies = new HashSet<>();

    protected AppUser() {
    }

    private AppUser(UUID companyId, String email, String fullName, String jobTitle, String passwordHash, boolean superAdmin) {
        this.companyId = companyId;
        this.email = email;
        this.fullName = fullName;
        this.jobTitle = jobTitle;
        this.passwordHash = passwordHash;
        this.superAdmin = superAdmin;
    }

    public static AppUser companyUser(UUID companyId, String email, String fullName, String jobTitle, String passwordHash) {
        return new AppUser(java.util.Objects.requireNonNull(companyId), email, fullName, jobTitle, passwordHash, false);
    }

    public static AppUser superAdmin(String email, String fullName, String passwordHash) {
        return new AppUser(null, email, fullName, "Super administrateur", passwordHash, true);
    }

    @PrePersist
    @PreUpdate
    protected void guardTenant() {
        TenantScope scope = TenantContext.current()
                .orElseThrow(() -> new IllegalStateException("Écriture sans portée"));
        if (scope instanceof TenantScope.Company c && !c.companyId().equals(companyId)) {
            throw new IllegalStateException("Écriture refusée : l'utilisateur appartient à une autre entreprise");
        }
    }

    // --- Règles de connexion -------------------------------------------------------------

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** Enregistre un échec ; verrouille le compte au-delà du seuil. Retourne vrai si le compte vient d'être verrouillé. */
    public boolean registerFailedLogin(Instant now) {
        failedLoginCount++;
        if (failedLoginCount >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = now.plus(LOCK_DURATION);
            failedLoginCount = 0;
            return true;
        }
        return false;
    }

    public void registerSuccessfulLogin(Instant now) {
        failedLoginCount = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }

    public void unlock() {
        failedLoginCount = 0;
        lockedUntil = null;
    }

    public void changePassword(String newHash) {
        this.passwordHash = newHash;
        this.passwordChangedAt = Instant.now();
        unlock();
    }

    public Set<Permission> effectivePermissions() {
        return roles.stream().flatMap(r -> r.getPermissions().stream()).collect(Collectors.toUnmodifiableSet());
    }

    // --- Mise à jour du profil ------------------------------------------------------------

    public void updateProfile(String fullName, String jobTitle) {
        this.fullName = fullName;
        this.jobTitle = jobTitle;
    }

    public void replaceRoles(Collection<Role> newRoles) {
        roles.clear();
        roles.addAll(newRoles);
    }

    public void replaceAgencies(Collection<Agency> newAgencies) {
        agencies.clear();
        agencies.addAll(newAgencies);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void assignDepartment(Department department) {
        this.department = department;
    }

    // --- Accesseurs -----------------------------------------------------------------------

    public UUID getCompanyId() {
        return companyId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Set<Role> getRoles() {
        return Set.copyOf(roles);
    }

    public Set<Agency> getAgencies() {
        return Set.copyOf(agencies);
    }

    public Department getDepartment() {
        return department;
    }
}
