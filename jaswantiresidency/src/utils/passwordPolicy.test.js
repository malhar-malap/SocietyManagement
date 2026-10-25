import { passwordPolicyError } from './passwordPolicy';

test('accepts 8+ characters with a letter and a digit', () => {
    expect(passwordPolicyError('Abcdefg1')).toBeNull();
});
test('rejects short, letter-only and digit-only passwords', () => {
    expect(passwordPolicyError('Ab1')).toMatch(/8 characters/);
    expect(passwordPolicyError('abcdefgh')).toMatch(/digit/);
    expect(passwordPolicyError('12345678')).toMatch(/letter/);
});
