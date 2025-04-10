package seedu.finbro.logic.command;

import seedu.finbro.model.Income;
import seedu.finbro.model.Expense;
import seedu.finbro.model.TransactionManager;
import seedu.finbro.model.Transaction;
import seedu.finbro.storage.Storage;
import seedu.finbro.ui.Ui;

import java.util.logging.Logger;
import java.text.DateFormatSymbols;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * Represents a command to view a financial summary.
 */
public class SummaryCommand implements Command {
    private static final int MAXIMUM_CATEGORIES_TO_DISPLAY = 3;
    private static final double DEFAULT_BUDGET = -1.0;
    private static final int MONTH_OFFSET = 1;
    private static final Logger logger = Logger.getLogger(SummaryCommand.class.getName());
    private final int month;
    private final int year;

    /**
     * Constructs a SummaryCommand with the specified month and end year.
     *
     * @param month the month in which transactions will be used for the summary
     * @param year the year in which transactions will be used for the summary
     */
    public SummaryCommand(int month, int year) {
        this.month = month;
        this.year = year;
    }

    /**
     * Executes the command to display a summary of the transactions in the specified month and year
     * Summary includes total income, total expenses, top 5 expense categories
     *
     * @param transactionManager The transaction manager to execute the command on
     * @param ui                 The UI to interact with the user
     * @param storage            The storage to save data
     * @return The string representation of the summary
     */
    @Override
    public String execute(TransactionManager transactionManager, Ui ui, Storage storage) {
        assert transactionManager != null : "TransactionManager cannot be null";
        assert ui != null : "UI cannot be null";
        assert storage != null : "Storage cannot be null";

        logger.info("Executing summary command");

        String monthString = new DateFormatSymbols().getMonths()[month-MONTH_OFFSET];
        double totalIncome = transactionManager.getMonthlyTotalIncome(month, year);
        double totalExpense = transactionManager.getMonthlyTotalExpense(month, year);
        logger.info(String.format("Calculating total income and total expenses for %s %d",
                monthString, year));
        String summaryDisplay = String.format("Financial Summary for %s %d:\n\n",  monthString, year);
        summaryDisplay += String.format(
                "Total Income: %s\n", seedu.finbro.util.CurrencyFormatter.format(totalIncome)
        );
        summaryDisplay += String.format(
                "Total Expenses: %s\n", seedu.finbro.util.CurrencyFormatter.format(totalExpense)
        );

        if (transactionManager.getBudget(month, year) == DEFAULT_BUDGET) {
            logger.info(("No budget found in hashmap"));
            summaryDisplay += String.format("\nNo budget set for %s %d\n", monthString, year);
        } else {
            double budget = transactionManager.getBudget(month, year);
            logger.info(String.format("Budget found in hashmap: %.2f", budget));
            summaryDisplay += String.format("\nBudget for %s %d: %s\n",
                    monthString,
                    year,
                    seedu.finbro.util.CurrencyFormatter.format(budget));
            double remainingBudget = budget - totalExpense;
            if (remainingBudget < 0) {
                summaryDisplay += String.format("Budget exceeded by: %s\n",
                        seedu.finbro.util.CurrencyFormatter.format(Math.abs(remainingBudget)));
            } else {
                summaryDisplay += String.format("Remaining budget: %s\n",
                        seedu.finbro.util.CurrencyFormatter.format(remainingBudget));
            }
        }

        logger.info(String.format("Calculating total expenses for top categories for %s %d",
                monthString, year));
        Map<Expense.Category, Double> sortedCategorisedExpenses =
                transactionManager.getMonthlyCategorisedExpenses(month, year)
                        .entrySet()
                        .stream()
                        .sorted(Map.Entry.<Expense.Category, Double> comparingByValue().reversed())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        if (!sortedCategorisedExpenses.isEmpty()) {
            summaryDisplay += "\nTop Expense Categories:\n";
            int categoryCount = 0;
            for (Map.Entry<Expense.Category, Double> expenseInCategory :
                    sortedCategorisedExpenses.entrySet()) {
                categoryCount++;
                assert expenseInCategory.getKey() != null: "Category cannot be null";
                assert expenseInCategory.getValue() <=
                        transactionManager.getMonthlyTotalExpense(month, year):
                        "Total expenses in one category cannot be greater " +
                                "than total expenses for the month";
                if (expenseInCategory.getValue() == 0) {
                    break;
                }
                summaryDisplay += String.format("%d. %s: %s\n", categoryCount,
                        expenseInCategory.getKey().toString(),
                        seedu.finbro.util.CurrencyFormatter.format(expenseInCategory.getValue()));
                if (categoryCount >= MAXIMUM_CATEGORIES_TO_DISPLAY) {
                    break;
                }
            }
        }

        // Get tagged transactions with detailed breakdown
        List<Transaction> monthlyTransactions = transactionManager.getMonthlyTransactions(month, year);

        // Separate maps for income and expense per tag
        Map<String, Double> taggedIncome = new HashMap<>();
        Map<String, Double> taggedExpenses = new HashMap<>();
        Map<String, Double> taggedNet = new HashMap<>();

        // Process each transaction
        for (Transaction transaction : monthlyTransactions) {
            List<String> tags = transaction.getTags();
            double amount = transaction.getAmount();

            for (String tag : tags) {
                // Process income transactions
                if (transaction instanceof Income) {
                    taggedIncome.put(tag, taggedIncome.getOrDefault(tag, 0.0) + amount);
                } else if (transaction instanceof Expense) {
                    taggedExpenses.put(tag, taggedExpenses.getOrDefault(tag, 0.0) + amount);
                }

                // Update the net amount (income - expenses) for each tag
                if (transaction instanceof Income) {
                    taggedNet.put(tag, taggedNet.getOrDefault(tag, 0.0) + amount);
                } else if (transaction instanceof Expense) {
                    taggedNet.put(tag, taggedNet.getOrDefault(tag, 0.0) - amount);
                }
            }
        }

        // Sort tags by the absolute value of combined income/expense amounts
        Map<String, Double> sortedTaggedNet = taggedNet.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        if (!sortedTaggedNet.isEmpty()) {
            summaryDisplay += "\nTags Summary:\n";
            int tagCount = 0;
            for (Map.Entry<String, Double> tagEntry : sortedTaggedNet.entrySet()) {
                tagCount++;
                String tag = tagEntry.getKey();
                double netAmount = tagEntry.getValue();
                double incomeAmount = taggedIncome.getOrDefault(tag, 0.0);
                double expenseAmount = taggedExpenses.getOrDefault(tag, 0.0);

                // Create a detailed breakdown for each tag
                summaryDisplay += String.format("%d. %s: %s (",
                        tagCount, tag, seedu.finbro.util.CurrencyFormatter.format(netAmount));

                if (incomeAmount > 0) {
                    summaryDisplay += String.format("Income: %s",
                            seedu.finbro.util.CurrencyFormatter.format(incomeAmount));

                    if (expenseAmount > 0) {
                        summaryDisplay += String.format(", Expense: %s",
                                seedu.finbro.util.CurrencyFormatter.format(expenseAmount));
                    }
                } else if (expenseAmount > 0) {
                    summaryDisplay += String.format("Expense: %s",
                            seedu.finbro.util.CurrencyFormatter.format(expenseAmount));
                }

                summaryDisplay += ")\n";
            }
        }

        return summaryDisplay;
    }

    /**
     * Returns false since this is not an exit command.
     *
     * @return false
     */
    @Override
    public boolean isExit() {
        return false;
    }
}
