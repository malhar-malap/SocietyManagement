import React, { useState } from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import OtpInput from './OtpInput';
import Modal from './Modal';
import StatusChip, { humanize } from './StatusChip';
import { FlowerLoader } from './Hibiscus';
import PasswordChecklist from '../auth/PasswordChecklist';
import { TextInput } from './Field';

const OtpHarness = ({ initial = '' }) => {
    const [value, setValue] = useState(initial);
    return (
        <>
            <OtpInput value={value} onChange={setValue} />
            <output data-testid="value">{value}</output>
        </>
    );
};

describe('OtpInput', () => {
    test('typing a digit moves to the next box', () => {
        render(<OtpHarness />);
        fireEvent.change(screen.getByLabelText('Digit 1 of 6'), { target: { value: '4' } });
        expect(screen.getByTestId('value').textContent).toBe('4');
        expect(document.activeElement).toBe(screen.getByLabelText('Digit 2 of 6'));
    });

    test('pasting a whole code fills every box and ignores non-digits', () => {
        render(<OtpHarness />);
        fireEvent.paste(screen.getByLabelText('Digit 1 of 6'), { clipboardData: { getData: () => '12 34-56' } });
        expect(screen.getByTestId('value').textContent).toBe('123456');
    });

    test('Backspace on an empty box clears the previous digit', () => {
        render(<OtpHarness initial="12" />);
        fireEvent.keyDown(screen.getByLabelText('Digit 3 of 6'), { key: 'Backspace' });
        expect(screen.getByTestId('value').textContent).toBe('1');
    });
});

describe('Modal', () => {
    test('is labelled by its title and closes on Escape', () => {
        const onClose = jest.fn();
        render(<Modal isOpen onClose={onClose} title="Record a payment"><input aria-label="Amount" /></Modal>);
        expect(screen.getByRole('dialog', { name: 'Record a payment' })).toBeInTheDocument();
        fireEvent.keyDown(document, { key: 'Escape' });
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    test('does not close on Escape while it is busy', () => {
        const onClose = jest.fn();
        render(<Modal isOpen onClose={onClose} title="Saving" dismissible={false}>Body</Modal>);
        fireEvent.keyDown(document, { key: 'Escape' });
        expect(onClose).not.toHaveBeenCalled();
    });

    test('renders nothing when closed', () => {
        render(<Modal isOpen={false} title="Hidden">Body</Modal>);
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
});

describe('StatusChip', () => {
    test('humanizes enum values', () => {
        expect(humanize('PARTIALLY_PAID')).toBe('Partially paid');
    });

    test.each([
        ['PAID', 'chip-success'],
        ['PAID_CASH', 'chip-success'],
        ['PENDING', 'chip-warning'],
        ['REJECTED', 'chip-danger'],
        ['SOMETHING_ELSE', 'chip-neutral'],
    ])('%s uses %s', (status, cls) => {
        render(<StatusChip status={status} />);
        expect(screen.getByText(humanize(status))).toHaveClass(cls);
    });
});

test('PasswordChecklist ticks each rule as it is met', () => {
    const { rerender } = render(<PasswordChecklist password="abc" />);
    expect(screen.getByText('Contains a letter').closest('li')).toHaveClass('ok');
    expect(screen.getByText('At least 8 characters').closest('li')).not.toHaveClass('ok');
    rerender(<PasswordChecklist password="abcdefg1" />);
    expect(screen.getAllByText('(met)')).toHaveLength(3);
});

test('FlowerLoader announces itself to screen readers', () => {
    render(<FlowerLoader label="Loading your bills" />);
    expect(screen.getByRole('status')).toHaveTextContent('Loading your bills');
});

test('TextInput wires its label, error and password toggle', () => {
    render(<TextInput label="Password" type="password" error="Too short" />);
    const input = screen.getByLabelText('Password');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('alert')).toHaveTextContent('Too short');
    fireEvent.click(screen.getByLabelText('Show password'));
    expect(input).toHaveAttribute('type', 'text');
});
