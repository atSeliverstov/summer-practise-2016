package Responses.utils;

import Responses.dao.AnswersDao;
import Responses.dao.DefaultAnswersDao;
import Responses.dao.FormsDao;
import Responses.dao.QuestionsDao;
import Responses.dbEntities.AnswersEntity;
import Responses.dbEntities.DefaultAnswersEntity;
import Responses.dbEntities.FormsEntity;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.hibernate.Session;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DocxWriter {
    /* Запись всех анкет в локальный .docx-файл на сервере, расположенный в filePath.
     * Сервлет должен просто переслать сгенерированный файл на сторону клиента. */
    public static void WriteForms(String filePath) throws IOException {
        XWPFDocument document = new XWPFDocument();

        Session session = HibernateSessionFactory.getSessionFactory().openSession();

        List<FormsEntity> forms = FormsDao.getForms();
        Collections.sort(forms, new Comparator<FormsEntity>() {
            public int compare(FormsEntity o1, FormsEntity o2) {
                int idDiff = o1.getId() - o2.getId();
                return idDiff != 0 ? idDiff : o1.getQuestionId() - o2.getQuestionId();
            }
        });
        if (!forms.isEmpty()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText("Form #0");
            paragraph.setIndentationHanging(200);
            run.addBreak();
            run.setFontSize(16);
            run.setBold(true);
            paragraph.addRun(run);
            int id = 0;
            for (FormsEntity fe : forms) {
                if (fe.getId() != id) {
                    run = paragraph.createRun();
                    run.addBreak(BreakType.PAGE);
                    paragraph.addRun(run);
                    id = fe.getId();
                    paragraph = document.createParagraph();
                    paragraph.setIndentationHanging(200);
                    run = paragraph.createRun();
                    run.setText("Form #" + id);
                    run.addBreak();
                    run.setFontSize(16);
                    run.setBold(true);
                    paragraph.addRun(run);
                }
                paragraph = document.createParagraph();
                run = paragraph.createRun();
                String qa = QuestionsDao.getQuestionById(fe.getQuestionId()).getValue()
                        + ": "
                        + AnswersDao.getAnswerById(fe.getAnswerId()).getValue();
                run.setText(qa);
                run.setFontSize(14);
                run.addBreak();
                paragraph.addRun(run);
            }
        }
        session.close();
        document.write(new FileOutputStream(filePath));
    }

    static private final int projectQuestionID = 0, postQuestionID = 4;
    public static void WriteFormsByProjectAndPost(String filePath, int projectNum, int postNum) throws IOException {
        XWPFDocument document = new XWPFDocument();

        Session session = HibernateSessionFactory.getSessionFactory().openSession();

        String project = projectNum == -1 ? null : DefaultAnswersDao.getAnswers().get(projectNum).getValue();
        String post = postNum == - 1 ? null : DefaultAnswersDao.getAnswers().get(postNum).getValue();

        List<AnswersEntity> answers = AnswersDao.getAnswers();
        if (!answers.isEmpty()) {
            ArrayList<Integer> projectIds = project == null ? null : new ArrayList<Integer>(answers.size() / 2);
            ArrayList<Integer> postIds = post == null ? null : new ArrayList<Integer>(answers.size() / 2);
            for (AnswersEntity ae : AnswersDao.getAnswers()) {
                if (ae.getValue().equals(project))
                    projectIds.add(ae.getId());
                if (ae.getValue().equals(post))
                    postIds.add(ae.getId());
            }

            List<FormsEntity> forms = FormsDao.getForms();
            Collections.sort(forms, new Comparator<FormsEntity>() {
                public int compare(FormsEntity o1, FormsEntity o2) {
                    int idDiff = o1.getId() - o2.getId();
                    return idDiff != 0 ? idDiff : o1.getQuestionId() - o2.getQuestionId();
                }
            });

            ArrayList<Integer> formIDs = new ArrayList<Integer>(forms.size());
            if (projectIds != null) {
                for (FormsEntity fe : forms) {
                    if (fe.getQuestionId() == projectQuestionID && projectIds.contains(fe.getAnswerId()))
                        formIDs.add(fe.getId());
                }
                if (postIds != null) {
                    for (FormsEntity fe : forms) {
                        if (fe.getQuestionId() == postQuestionID && !postIds.contains(fe.getAnswerId()))
                            formIDs.remove(new Integer(fe.getId()));
                    }
                }
            }
            else {
                for (FormsEntity fe : forms) {
                    if (fe.getQuestionId() == postQuestionID && postIds.contains(fe.getAnswerId()))
                        formIDs.add(fe.getId());
                }
            }

            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();

            if (!formIDs.isEmpty()) {
                int id = -1;
                for (FormsEntity fe : forms) {
                    if (fe.getId() != id && formIDs.contains(fe.getId())) {
                        id = fe.getId();
                        run.setText("Form #" + id);
                        paragraph.setIndentationHanging(200);
                        run.addBreak();
                        run.setFontSize(16);
                        run.setBold(true);
                        paragraph.addRun(run);
                    } else if (fe.getId() == id) {
                        paragraph = document.createParagraph();
                        run = paragraph.createRun();
                        String qa = QuestionsDao.getQuestionById(fe.getQuestionId()).getValue()
                                + ": "
                                + AnswersDao.getAnswerById(fe.getAnswerId()).getValue();
                        run.setText(qa);
                        run.setFontSize(14);
                        run.addBreak();
                        paragraph.addRun(run);
                    }
                }
            }
        }
        session.close();
        document.write(new FileOutputStream(filePath));
    }
}
