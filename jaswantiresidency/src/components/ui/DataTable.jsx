import React, { useState } from 'react';
import { Search, ChevronLeft, ChevronRight } from 'lucide-react';
import './ui.css';

const DataTable = ({ columns, data, onSearch, emptyMessage = 'No data available', isLoading = false }) => {
    const [searchTerm, setSearchTerm] = useState('');
    
    // Very basic pagination logic placeholder for frontend stub
    const [currentPage, setCurrentPage] = useState(1);
    const rowsPerPage = 5;
    const totalPages = Math.ceil((data?.length || 0) / rowsPerPage);
    
    const paginatedData = data?.slice((currentPage - 1) * rowsPerPage, currentPage * rowsPerPage);

    const handleSearch = (e) => {
        setSearchTerm(e.target.value);
        if (onSearch) onSearch(e.target.value);
    };

    return (
        <div className="data-table-container">
            <div className="table-toolbar">
                <div className="search-wrapper">
                    <Search size={18} className="search-icon" />
                    <input 
                        type="text" 
                        placeholder="Search..." 
                        value={searchTerm}
                        onChange={handleSearch}
                        className="table-search"
                    />
                </div>
            </div>
            
            <div className="table-responsive">
                <table className="custom-table">
                    <thead>
                        <tr>
                            {columns.map((col, idx) => (
                                <th key={idx}>{col.header}</th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {isLoading ? (
                            <tr>
                                <td colSpan={columns.length} className="text-center py-8">
                                    <div className="spinner mx-auto"></div>
                                </td>
                            </tr>
                        ) : paginatedData?.length > 0 ? (
                            paginatedData.map((row, idx) => (
                                <tr key={idx}>
                                    {columns.map((col, colIdx) => (
                                        <td key={colIdx}>
                                            {col.render ? col.render(row) : row[col.accessor]}
                                        </td>
                                    ))}
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={columns.length} className="text-center py-8 empty-state">
                                    {emptyMessage}
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
            
            {!isLoading && totalPages > 0 && (
                <div className="pagination">
                    <span className="page-info">Page {currentPage} of {totalPages}</span>
                    <div className="page-controls">
                        <button 
                            className="page-btn" 
                            disabled={currentPage === 1}
                            onClick={() => setCurrentPage(p => p - 1)}
                        >
                            <ChevronLeft size={16} />
                        </button>
                        <button 
                            className="page-btn" 
                            disabled={currentPage === totalPages}
                            onClick={() => setCurrentPage(p => p + 1)}
                        >
                            <ChevronRight size={16} />
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default DataTable;
