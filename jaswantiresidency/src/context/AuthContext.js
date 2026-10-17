import React, { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Check if user is logged in on mount
        const token = localStorage.getItem('accessToken');
        const role = localStorage.getItem('role');
        const name = localStorage.getItem('name');
        const userId = localStorage.getItem('userId');
        const roomId = localStorage.getItem('roomId');
        const forcePasswordChange = localStorage.getItem('forcePasswordChange') === 'true';

        if (token && role) {
            setUser({ role, name, userId, roomId, forcePasswordChange });
        }
        setLoading(false);
    }, []);

    const login = (data) => {
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);
        localStorage.setItem('role', data.role);
        localStorage.setItem('name', data.name);
        if (data.userId) localStorage.setItem('userId', data.userId);
        if (data.roomId) localStorage.setItem('roomId', data.roomId);
        if (data.forcePasswordChange !== undefined) localStorage.setItem('forcePasswordChange', data.forcePasswordChange);
        
        setUser({ 
            role: data.role, 
            name: data.name, 
            userId: data.userId, 
            roomId: data.roomId, 
            forcePasswordChange: data.forcePasswordChange 
        });
    };

    const logout = () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('role');
        localStorage.removeItem('name');
        localStorage.removeItem('userId');
        localStorage.removeItem('roomId');
        localStorage.removeItem('forcePasswordChange');
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, logout, loading }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
