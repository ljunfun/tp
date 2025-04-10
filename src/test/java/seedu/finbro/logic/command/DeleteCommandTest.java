package seedu.finbro.logic.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import seedu.finbro.model.Expense;
import seedu.finbro.model.Income;
import seedu.finbro.model.TransactionManager;
import seedu.finbro.storage.Storage;
import seedu.finbro.ui.Ui;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests for the DeleteCommand class.
 */
class DeleteCommandTest {
    private TransactionManager transactionManager;
    private Ui ui;
    private Storage storage;

    @BeforeEach
    public void setUp() {
        transactionManager = new TransactionManager();
        ui = new Ui();
        storage = new Storage();

        // Add some test transactions
        transactionManager.addTransaction(new Income(1000, "Salary", Collections.emptyList()));
        transactionManager.addTransaction(new Expense(50, "Groceries", Expense.Category.FOOD, Collections.emptyList()));
        transactionManager.addTransaction(new Expense(20,
                "Bus fare", Expense.Category.TRANSPORT, Collections.emptyList()));
    }

    @Test
    void executeValidIndexSuccess() {
        DeleteCommand command = new DeleteCommand(2,2);
        String result = command.execute(transactionManager, ui, storage);
        
        assertEquals(2, transactionManager.getTransactionCount());
        assertEquals("Deleted transactions:\n- [Expense][Food] $50.00 - Groceries", result);
    }

    @Test
    void executeInvalidIndexTooLargeError() {
        DeleteCommand command = new DeleteCommand(4,4);
        String result = command.execute(transactionManager, ui, storage);
        
        assertEquals(3, transactionManager.getTransactionCount());
        assertEquals("Invalid index range. There are only 3 transactions.", result);
    }

    @Test
    void executeInvalidIndexZeroError() {
        DeleteCommand command = new DeleteCommand(0,0);
        String result = command.execute(transactionManager, ui, storage);
        
        assertEquals(3, transactionManager.getTransactionCount());
        assertEquals("Invalid index range. There are only 3 transactions.", result);
    }

    @Test
    void executeInvalidIndexNegativeError() {
        DeleteCommand command = new DeleteCommand(-1,-1);
        String result = command.execute(transactionManager, ui, storage);
        
        assertEquals(3, transactionManager.getTransactionCount());
        assertEquals("Invalid index range. There are only 3 transactions.", result);
    }

    @Test
    void isExitReturnsFalse() {
        DeleteCommand command = new DeleteCommand(1,1);
        assertFalse(command.isExit());
    }
}
