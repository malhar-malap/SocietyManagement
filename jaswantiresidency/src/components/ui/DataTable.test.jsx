import { render, screen, fireEvent } from '@testing-library/react';
import DataTable from './DataTable';

const columns = [{ header: 'Name', accessor: 'name' }, { header: 'Room', accessor: 'room' }];
const rows = Array.from({ length: 25 }, (_, i) => ({ id: i + 1, name: `Person ${i + 1}`, room: `C-${100 + i}` }));

test('shows 10 rows per page with a count', () => {
    render(<DataTable columns={columns} data={rows} />);
    expect(screen.getByText('Showing 1–10 of 25')).toBeInTheDocument();
    expect(screen.getByText('Person 1')).toBeInTheDocument();
    expect(screen.queryByText('Person 11')).not.toBeInTheDocument();
});

test('search filters rows and resets to page 1', () => {
    render(<DataTable columns={columns} data={rows} />);
    fireEvent.click(screen.getByLabelText('Next page'));
    expect(screen.getByText('Showing 11–20 of 25')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Search table'), { target: { value: 'C-124' } });
    expect(screen.getByText('Person 25')).toBeInTheDocument();
    expect(screen.getByText('Showing 1–1 of 1')).toBeInTheDocument();
});

test('shows a no-results message when nothing matches', () => {
    render(<DataTable columns={columns} data={rows} />);
    fireEvent.change(screen.getByLabelText('Search table'), { target: { value: 'zzz' } });
    expect(screen.getByText('No results for "zzz"')).toBeInTheDocument();
});
