package com.company.imaging.web.screens;

import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.app.core.file.FileUploadDialog;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.InputDialog;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.export.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;

import javax.inject.Inject;
import java.io.*;
import java.util.*;

@UiController("imaging_")
@UiDescriptor("new-screen.xml")
public class NewScreen extends Screen {
    @Inject
    private TextField<Integer> num;
    @Inject
    private TextField<String> a1;
    @Inject
    private TextField<String> a2;
    @Inject
    private Label<String> result;
    @Inject
    private Label<String> taskDescription;
    @Inject
    private LookupField<String> tasks;

    @Inject
    ExportDisplay exportDisplay;
    @Inject
    Notifications notifications;
    @Inject
    private Screens screens;
    @Inject
    private FileUploadingAPI fileUploadingAPI;
    @Inject
    private Dialogs dialogs;

    @Subscribe
    protected void onInit(InitEvent event) {
        List<String> list = new ArrayList<>();
        list.add("Задача 1");
        list.add("Задача 2");
        tasks.setOptionsList(list);
        num.setValue(1);
    }

    @Subscribe("tasks")
    public void onTasksValueChange(HasValue.ValueChangeEvent event) {
        switch (tasks.getValue()) {
            case "Задача 1":
                a1.setVisible(true);
                a2.setVisible(true);
                num.setVisible(false);
                taskDescription.setValue("Введите два массива строк a1 и a2.\nПосле нажатия кнопки посчитать появится " +
                        "отсортированный в лексикографическом порядке массив строк a1, которые являются подстроками строк a2.");
                break;
            case "Задача 2":
                a1.setVisible(false);
                a2.setVisible(false);
                num.setVisible(true);
                taskDescription.setValue("Введите положительное целое число num.\nПосле нажатия кнопки посчитать появится" +
                        "развёрнутый вид num");
                break;
        }
        result.setValue("");
    }

    @Subscribe("calculate")
    public void onCalculateClick(Button.ClickEvent event) {
        result.setValue("");
        try {
            switch (tasks.getValue()) {
                case "Задача 1":
                    doTask1();
                    break;
                case "Задача 2":
                    doTask2();
                    break;
            }
        } catch (NullPointerException e) {
            notifications.create().withCaption("Выберите задачу").show();
        }
    }

    private void doTask1() {
        try {
            String[] mass1 = a1.getValue().split(" ");
            ArrayList<String> answer = new ArrayList<String>();
            for (String s : mass1) {
                if (a2.getValue().toLowerCase().contains(s.toLowerCase())) {
                    answer.add(s);
                }
            }
            Collections.sort(answer);
            result.setValue("Результат:    " + String.join(" ", answer));
        } catch (NullPointerException e) {
            notifications.create().withCaption("Введите данные").show();
        }
    }

    private void doTask2() {
        try {
            int n = this.num.getValue();
            String str = "";
            int ost = 0;
            for (int i = 10; i < n; i = i * 10) {
                if (n % i != 0) {
                    str = " + " + (n % i - ost) + str;
                    ost = n % i;
                }
            }
            str = (n - ost) + str;
            result.setValue("Результат:    " + str);
        } catch (NullPointerException e) {
            notifications.create().withCaption("Введите данные").show();
        }
    }

    @Subscribe("upload")
    protected void onUploadClick(Button.ClickEvent event) {
        FileUploadDialog dialog = (FileUploadDialog) screens.create("fileUploadDialog", OpenMode.DIALOG);
        dialog.setCaption("Выберите файл");
        dialog.addCloseWithCommitListener(() -> {
            UUID fileId = dialog.getFileId();
            File file = fileUploadingAPI.getFile(fileId);
            BufferedReader br;
            try {
                br = new BufferedReader(new FileReader(file));
                String str = br.readLine();
                switch (str) {
                    case "Задача 1":
                        tasks.setValue("Задача 1");
                        a1.setValue(br.readLine());
                        a2.setValue(br.readLine());
                        break;
                    case "Задача 2":
                        tasks.setValue("Задача 2");
                        num.setValue(Integer.valueOf(br.readLine()));
                        break;
                }
                result.setValue("");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        screens.show(dialog);
    }

    @Subscribe("save")
    public void onSaveClick(Button.ClickEvent event) {
        String str = null;
        try {
            switch (tasks.getValue()) {
                case "Задача 1":
                    str = tasks.getValue() + "\n" + (a1.getValue() != null ? a1.getValue() : "") + "\n" + (a2.getValue() != null ? a2.getValue() : "");
                    break;
                case "Задача 2":
                    str = tasks.getValue() + "\n" + (num.getValue() != null ? num.getValue() : "");
                    break;
            }
        } catch (NullPointerException e) {
            notifications.create().withCaption("Выберите задачу").show();
        }
        if (str != null) {
            String finalStr = str;
            dialogs.createInputDialog(this)
                    .withCaption("Введите имя файла")
                    .withParameters(
                            InputParameter.stringParameter("name")
                                    .withCaption("Имя").withRequired(true)
                    )
                    .withActions(DialogActions.OK_CANCEL)
                    .withCloseListener(closeEvent -> {
                        if (closeEvent.getCloseAction().equals(InputDialog.INPUT_DIALOG_OK_ACTION)) {
                            String name = closeEvent.getValue("name");
                            byte[] bytes;
                            try {
                                bytes = finalStr.getBytes("UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                throw new RuntimeException(e);
                            }
                            exportDisplay.show(new ByteArrayDataProvider(bytes), name + ".txt", ExportFormat.TEXT);
                            notifications.create().withCaption("Задача была сохранена в файле " + name + ".txt").show();
                        }
                    }).show();
        }
    }

    @Subscribe("num")
    public void onNumTextChange(TextInputField.TextChangeEvent event) {
        result.setValue("");
        try {
            num.validate();
        } catch (ValidationException e) {
            num.setValue(1);
        }
    }

    @Subscribe("num")
    public void onNumValueChange(HasValue.ValueChangeEvent<Integer> event) {
        try {
            num.validate();
        } catch (ValidationException | NullPointerException e) {
            num.setValue(1);
        }
    }

    @Subscribe("a1")
    public void onA1TextChange(TextInputField.TextChangeEvent event) {
        result.setValue("");
    }

    @Subscribe("a2")
    public void onA2TextChange(TextInputField.TextChangeEvent event) {
        result.setValue("");
    }
}