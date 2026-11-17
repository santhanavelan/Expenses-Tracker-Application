import java.awt.BorderLayout;
  import java.awt.Color;
  import java.awt.FlowLayout;
  import java.awt.Font;
  import java.awt.GridLayout;
  import java.io.File;
  import java.io.IOException;
  import java.io.PrintWriter;
  import java.nio.charset.StandardCharsets;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.nio.file.Paths;
 import java.time.LocalDate;
 import java.time.YearMonth;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeMap;
 import javax.swing.DefaultRowSorter;
 import javax.swing.JButton;
 import javax.swing.JComboBox;
 import javax.swing.JFileChooser;
 import javax.swing.JFrame;
 import javax.swing.JLabel;
 import javax.swing.JOptionPane;
 import javax.swing.JPanel;
 import javax.swing.JScrollPane;
 import javax.swing.JTable;
 import javax.swing.JTextField;
 import javax.swing.ListSelectionModel;
 import javax.swing.RowFilter;
 import javax.swing.SwingUtilities;
 import javax.swing.event.DocumentEvent;
 import javax.swing.event.DocumentListener;
 import javax.swing.table.DefaultTableModel;
 import javax.swing.table.TableRowSorter;

 public class ExpenseTracker extends JFrame {
 private final Path data = Paths.get("data/transactions.txt");
 private final Path budgetFile = Paths.get("data/budgets.txt");
 private final DefaultTableModel model = new DefaultTableModel(
 new String[]{"ID", "Date", "Type", "Category", "Amount", "Description"}, 0) {
 @Override
 public boolean isCellEditable(int r, int c) {
 return false;
 }
 };
 private final JTable table = new JTable(model);
 private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
 private final JTextField date = new JTextField(LocalDate.now().toString(), 10);
 private final JTextField amount = new JTextField(8);
 private final JTextField description = new JTextField(15);
 private final JTextField budgetAmount = new JTextField(7);
 private final JTextField search = new JTextField(12);
 private final JComboBox<String> type = new JComboBox<>(new String[]{"EXPENSE", "INCOME"});
 private final JComboBox<String> category = new JComboBox<>(new String[]{
 "Food", "Travel", "Shopping", "Bills", "Health", "Entertainment", "Salary", "Other"});
 private final JLabel summary = new JLabel();
 private final JLabel status = new JLabel("Ready");
 private final Map<String, Double> budgets = new TreeMap<>();

 public ExpenseTracker() {
 super("Expense Tracker Application");
 setDefaultCloseOperation(EXIT_ON_CLOSE);
 setSize(900, 560);
 setLocationRelativeTo(null);

 category.setEditable(true);
 table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
 table.setRowSorter(sorter);

 JPanel top = new JPanel(new BorderLayout(8, 8));


 summary.setFont(new Font("SansSerif", Font.BOLD, 15));
 top.add(summary, BorderLayout.NORTH);

 JPanel form = new JPanel(new GridLayout(2, 6, 6, 6));
 form.add(new JLabel("Date (YYYY-MM-DD)"));
 form.add(new JLabel("Type"));
 form.add(new JLabel("Category"));
 form.add(new JLabel("Amount"));
 form.add(new JLabel("Description"));
 form.add(new JLabel("Budget"));
 form.add(date);
 form.add(type);
 form.add(category);
 form.add(amount);
 form.add(description);
 form.add(budgetAmount);
 top.add(form, BorderLayout.CENTER);

 JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
 addButton(buttons, "Add", this::addTransaction);
 addButton(buttons, "Update", this::updateTransaction);
 addButton(buttons, "Delete", this::deleteTransaction);
 addButton(buttons, "Clear", this::clearForm);
 addButton(buttons, "Set Budget", this::setBudget);
 addButton(buttons, "Report", this::showReport);
 addButton(buttons, "Export CSV", this::exportCsv);
 buttons.add(new JLabel("Search:"));
buttons.add(search);

search.getDocument().addDocumentListener(new DocumentListener() {
@Override
public void insertUpdate(DocumentEvent e) {
filterRows();
}

@Override
public void removeUpdate(DocumentEvent e) {
filterRows();
}

@Override
public void changedUpdate(DocumentEvent e) {
filterRows();
}
});

JPanel north = new JPanel(new BorderLayout());
north.add(top, BorderLayout.NORTH);
north.add(buttons, BorderLayout.SOUTH);
add(north, BorderLayout.NORTH);
add(new JScrollPane(table), BorderLayout.CENTER);
status.setForeground(new Color(35, 90, 35));
add(status, BorderLayout.SOUTH);

table.getSelectionModel().addListSelectionListener(e -> {
if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
loadSelected();
}
});

loadData();
refresh();
}

private void addButton(JPanel panel, String text, Runnable action) {
JButton button = new JButton(text);
button.addActionListener(e -> action.run());
panel.add(button);
}

private void addTransaction() {
try {


String d = LocalDate.parse(date.getText().trim()).toString();
double value = Double.parseDouble(amount.getText().trim());
if (value <= 0) {
throw new IllegalArgumentException("Amount must be greater than zero.");
}
String cat = category.getSelectedItem().toString().trim();
if (cat.isEmpty()) {
throw new IllegalArgumentException("Enter a category.");
}
model.addRow(new Object[]{nextId(), d, type.getSelectedItem(), cat, value,
description.getText().trim()});
saveData();
clearForm();
refresh();
} catch (Exception ex) {
error("Enter a valid date and positive amount. " + ex.getMessage());
}
}

private void updateTransaction() {
int row = table.getSelectedRow();
if (row < 0) {
error("Select a transaction to update.");
return;
}
try {
String d = LocalDate.parse(date.getText().trim()).toString();
double value = Double.parseDouble(amount.getText().trim());
String cat = category.getSelectedItem().toString().trim();
if (value <= 0 || cat.isEmpty()) {
throw new IllegalArgumentException();
}
int r = table.convertRowIndexToModel(row);
model.setValueAt(d, r, 1);
model.setValueAt(type.getSelectedItem(), r, 2);
model.setValueAt(cat, r, 3);
model.setValueAt(value, r, 4);
model.setValueAt(description.getText().trim(), r, 5);
saveData();
refresh();
status.setText("Transaction updated.");
} catch (Exception ex) {
error("Enter a valid date, category and positive amount.");
}
}

private void deleteTransaction() {
int row = table.getSelectedRow();
if (row < 0) {
error("Select a transaction to delete.");
return;
}
model.removeRow(table.convertRowIndexToModel(row));
saveData();
refresh();
clearForm();
status.setText("Transaction deleted.");
}

private void loadSelected() {
int r = table.convertRowIndexToModel(table.getSelectedRow());
date.setText(model.getValueAt(r, 1).toString());
type.setSelectedItem(model.getValueAt(r, 2));
category.setSelectedItem(model.getValueAt(r, 3));
amount.setText(model.getValueAt(r, 4).toString());
description.setText(model.getValueAt(r, 5).toString());
}

private void clearForm() {
table.clearSelection();
date.setText(LocalDate.now().toString());
amount.setText("");


description.setText("");
type.setSelectedIndex(0);
budgetAmount.setText("");
}

private int nextId() {
int max = 0;
for (int r = 0; r < model.getRowCount(); r++) {
max = Math.max(max, Integer.parseInt(model.getValueAt(r, 0).toString()));
}
return max + 1;
}

private void loadData() {
try {
Files.createDirectories(data.getParent());
if (Files.exists(data)) {
for (String line : Files.readAllLines(data, StandardCharsets.UTF_8)) {
String[] p = line.split("\\|", 6);
if (p.length == 6) {
model.addRow(new Object[]{Integer.parseInt(p[0]), p[1], p[2],
p[3], Double.parseDouble(p[4]), p[5]});
}
}
}
if (Files.exists(budgetFile)) {
for (String line : Files.readAllLines(budgetFile, StandardCharsets.UTF_8)) {
String[] p = line.split("\\|", 2);
if (p.length == 2) {
budgets.put(p[0], Double.parseDouble(p[1]));
}
}
}
} catch (Exception ex) {
error("Some saved data could not be loaded.");
}
}

private void saveData() {
try {
Files.createDirectories(data.getParent());
List<String> lines = new ArrayList<>();
for (int r = 0; r < model.getRowCount(); r++) {
StringBuilder line = new StringBuilder();
for (int c = 0; c < 6; c++) {
if (c > 0) {
line.append('|');
}
line.append(model.getValueAt(r, c).toString()
.replace("|", "/").replace("\n", " "));
}
lines.add(line.toString());
}
Files.write(data, lines, StandardCharsets.UTF_8);

List<String> bs = new ArrayList<>();
for (Map.Entry<String, Double> b : budgets.entrySet()) {
bs.add(b.getKey() + "|" + b.getValue());
}
Files.write(budgetFile, bs, StandardCharsets.UTF_8);
} catch (IOException ex) {
error("Could not save data: " + ex.getMessage());
}
}

private void setBudget() {
try {
String cat = category.getSelectedItem().toString().trim();
double limit = Double.parseDouble(budgetAmount.getText().trim());
if (cat.isEmpty() || limit <= 0) {
throw new IllegalArgumentException();
}


budgets.put(cat, limit);
saveData();
refresh();
status.setText("Budget saved for " + cat + ".");
} catch (Exception ex) {
error("Enter a category and a positive budget amount.");
}
}

private void refresh() {
double income = 0, expense = 0;
for (int r = 0; r < model.getRowCount(); r++) {
double value = Double.parseDouble(model.getValueAt(r, 4).toString());
if ("INCOME".equals(model.getValueAt(r, 2))) {
income += value;
} else {
expense += value;
}
}
summary.setText(String.format("Income: %.2f   Expenses: %.2f   Balance: %.2f",
income, expense, income - expense));
String alert = budgetAlert();
status.setForeground(alert.isEmpty() ? new Color(35, 90, 35) : Color.RED);
status.setText(alert.isEmpty() ? "Ready" : alert);
}

private String budgetAlert() {
YearMonth month = YearMonth.now();
for (Map.Entry<String, Double> b : budgets.entrySet()) {
double spent = 0;
for (int r = 0; r < model.getRowCount(); r++) {
if ("EXPENSE".equals(model.getValueAt(r, 2))
&& b.getKey().equals(model.getValueAt(r, 3))) {
try {
LocalDate d = LocalDate.parse(model.getValueAt(r, 1).toString());
if (YearMonth.from(d).equals(month)) {
spent += Double.parseDouble(model.getValueAt(r, 4).toString());
}
} catch (Exception ignored) {
}
}
}
if (spent > b.getValue()) {
return "BUDGET EXCEEDED: " + b.getKey();
}
}
return "";
}

private void filterRows() {
String q = search.getText().trim();
sorter.setRowFilter(q.isEmpty() ? null
: RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q)));
}

private void showReport() {
double income = 0, expense = 0;
Map<String, Double> byCategory = new TreeMap<>();
YearMonth month = YearMonth.now();
for (int r = 0; r < model.getRowCount(); r++) {
try {
LocalDate d = LocalDate.parse(model.getValueAt(r, 1).toString());
if (!YearMonth.from(d).equals(month)) {
continue;
}
double value = Double.parseDouble(model.getValueAt(r, 4).toString());
if ("INCOME".equals(model.getValueAt(r, 2))) {
income += value;
} else {
expense += value;
String c = model.getValueAt(r, 3).toString();
byCategory.put(c, byCategory.getOrDefault(c, 0.0) + value);


}
} catch (Exception ignored) {
}
}
StringBuilder report = new StringBuilder("MONTHLY REPORT - ")
.append(month).append("\n\n")
.append(String.format("Income: %.2f%nExpenses: %.2f%nBalance: %.2f%n%nBy category:%n",
income, expense, income - expense));
byCategory.forEach((k, v) -> report.append(String.format("%-18s %.2f%n", k, v)));
JOptionPane.showMessageDialog(this, report.toString(), "Expense Report",
JOptionPane.INFORMATION_MESSAGE);
}

private void exportCsv() {
JFileChooser chooser = new JFileChooser();
chooser.setSelectedFile(new File("expenses.csv"));
if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
return;
}
try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(
chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8))) {
out.println("ID,Date,Type,Category,Amount,Description");
for (int r = 0; r < model.getRowCount(); r++) {
for (int c = 0; c < 6; c++) {
if (c > 0) {
out.print(',');
}
out.print(csv(model.getValueAt(r, c).toString()));
}
out.println();
}
status.setText("CSV exported successfully.");
} catch (IOException ex) {
error("CSV export failed: " + ex.getMessage());
}
}

private String csv(String s) {
return "\"" + s.replace("\"", "\"\"") + "\"";
}

private void error(String message) {
JOptionPane.showMessageDialog(this, message, "Expense Tracker",
JOptionPane.ERROR_MESSAGE);
}

public static void main(String[] args) {
SwingUtilities.invokeLater(() -> new ExpenseTracker().setVisible(true));
}
}
