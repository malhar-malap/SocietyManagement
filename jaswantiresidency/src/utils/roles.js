// Staff roles. Office assistants work in the admin area but have no Community section (helpdesk, announcements).
export const STAFF_ROLES = ['ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT'];
export const COMMUNITY_ROLES = ['ADMIN', 'MASTER_ADMIN'];

export const ROLE_NAMES = {
    MASTER_ADMIN: 'Master admin',
    ADMIN: 'Admin',
    OFFICE_ASSISTANT: 'Office assistant',
    RESIDENT: 'Resident',
};

export const isStaff = (role) => STAFF_ROLES.includes(role);
export const canAccessCommunity = (role) => COMMUNITY_ROLES.includes(role);
export const homeFor = (role) => (isStaff(role) ? '/admin/dashboard' : '/resident/dashboard');
