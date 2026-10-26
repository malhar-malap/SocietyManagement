// Mirrors the server's PasswordValidator so users see the rule before submitting
export const PASSWORD_HINT = 'At least 8 characters, with a letter and a digit';

/** Returns an error message, or null when the password satisfies the policy. */
export const passwordPolicyError = (password) => {
    if (!password || password.length < 8) return 'Password must be at least 8 characters long.';
    if (!/[A-Za-z]/.test(password)) return 'Password must contain at least one letter.';
    if (!/\d/.test(password)) return 'Password must contain at least one digit.';
    return null;
};
