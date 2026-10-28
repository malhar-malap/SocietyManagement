import { canAccessCommunity, homeFor, isStaff } from './roles';

test('office assistants are staff without the community section', () => {
    expect(isStaff('OFFICE_ASSISTANT')).toBe(true);
    expect(canAccessCommunity('OFFICE_ASSISTANT')).toBe(false);
    expect(canAccessCommunity('ADMIN')).toBe(true);
    expect(canAccessCommunity('MASTER_ADMIN')).toBe(true);
    expect(isStaff('RESIDENT')).toBe(false);
    expect(homeFor('OFFICE_ASSISTANT')).toBe('/admin/dashboard');
    expect(homeFor('RESIDENT')).toBe('/resident/dashboard');
});
