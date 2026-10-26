import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Login from './Login';
import { AuthProvider } from '../context/AuthContext';
import { login as apiLogin } from '../api/authApi';

jest.mock('../api/authApi', () => ({ login: jest.fn() }));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({ ...jest.requireActual('react-router-dom'), useNavigate: () => mockNavigate }));

const renderLogin = () => render(
    <AuthProvider>
        <MemoryRouter><Login /></MemoryRouter>
    </AuthProvider>,
);

beforeEach(() => { localStorage.clear(); jest.clearAllMocks(); });

test('asks for both fields before calling the server', async () => {
    renderLogin();
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Enter your email or flat number and your password.');
    expect(apiLogin).not.toHaveBeenCalled();
});

test('shows a friendly message for wrong credentials and clears the password', async () => {
    apiLogin.mockRejectedValue({ response: { status: 401, data: { message: 'Invalid Username or Password' } } });
    renderLogin();
    fireEvent.change(screen.getByLabelText('Email or flat number'), { target: { value: 'a@b.co' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'nope1234' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    expect(await screen.findByText('That email, flat number or password is not right. Please try again.')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toHaveValue('');
});

test('first-time users go straight to account setup', async () => {
    apiLogin.mockResolvedValue({ accessToken: 't', refreshToken: 'r', role: 'RESIDENT', name: 'Neha', userId: 5, forcePasswordChange: true });
    renderLogin();
    fireEvent.change(screen.getByLabelText('Email or flat number'), { target: { value: 'C-301' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'Temp1234' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/setup', { replace: true }));
    expect(apiLogin).toHaveBeenCalledWith({ identifier: 'C-301', password: 'Temp1234' });
});
