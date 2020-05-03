package medapp.service.impl;

import medapp.dao.api.AssignmentDAO;
import medapp.dao.api.EventDAO;
import medapp.dao.api.PatientDAO;
import medapp.dto.AssignmentDto;
import medapp.exceptions.DaoException;
import medapp.exceptions.ErrorService;
import medapp.exceptions.ServiceException;
import medapp.model.Assignment;
import medapp.model.Event;
import medapp.model.Patient;
import medapp.service.api.AssignmentService;
import medapp.service.api.EventService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
public class AssignmentServiceImpl implements AssignmentService {

    private static final Logger LOGGER = Logger.getLogger(AssignmentService.class);
    private AssignmentDAO assignmentDAO;
    private PatientDAO patientDAO;
    private EventDAO eventDAO;

    @Autowired
    public void setEventDAO(EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    @Autowired
    public void setAssignmentDAO(AssignmentDAO assignmentDAO) {
        this.assignmentDAO = assignmentDAO;
    }

    @Autowired
    public void setPatientDAO(PatientDAO patientDAO) {
        this.patientDAO = patientDAO;
    }

    /**
     * convert AssignmentDto to Assignment and
     * transfer Assignment to dao.add().
     * Then generate Events by this Assignment.
     *
     * @param assignmentDto
     * @throws ServiceException
     */
    @Override
    @Transactional
    public void add(AssignmentDto assignmentDto) throws ServiceException {
        try {
            Patient patient = patientDAO.getById(assignmentDto.getPatientId());
            Assignment assignment = new Assignment();
            assignment.setPatient(patient);
            //a.setId(assignmentDto.getId());
            assignment.setName(assignmentDto.getName());
            assignment.setType(assignmentDto.getType());
            //create period
            assignment.setPeriod(assignmentDto.getDateFrom() + " - " + assignmentDto.getDateTo());
            assignment.setDoze(assignmentDto.getDoze());
            //time pattern
            StringBuilder tp = new StringBuilder();
            for (String s : assignmentDto.getWeeks()) {
                tp.append(s);
                tp.append(" ");
            }
            assignment.setTimePattern(tp.toString());
            //set time
            assignment.setTime1(assignmentDto.getTime1());
            assignment.setTime2(assignmentDto.getTime2());
            assignment.setTime3(assignmentDto.getTime3());
            assignment.setDateFrom(assignmentDto.getDateFrom());
            assignment.setDateTo(assignmentDto.getDateTo());
            assignmentDAO.add(assignment);
            generateEventsByAssId(assignment.getId());
        } catch (DaoException e) {
            throw new ServiceException(ErrorService.DATABASE_EXCEPTION, e);
        }
    }

    /**
     * Convert AssignmentDto to Assignment
     * and transfer Assignment to dao.update().
     * Then generate events by this Assignment.
     *
     * @param assignmentDto
     */
    @Override
    @Transactional
    public void update(AssignmentDto assignmentDto) {
        Assignment a = assignmentDAO.getById(assignmentDto.getId());
        a.setType(assignmentDto.getType());
        a.setName(assignmentDto.getName());
        a.setPeriod(assignmentDto.getDateFrom() + " - " + assignmentDto.getDateTo());
        a.setDateFrom(assignmentDto.getDateFrom());
        a.setDateTo(assignmentDto.getDateTo());
        a.setDoze(assignmentDto.getDoze());
        a.setTime1(assignmentDto.getTime1());
        a.setTime2(assignmentDto.getTime2());
        a.setTime3(assignmentDto.getTime3());
        StringBuilder tp = new StringBuilder();
        for (String s : assignmentDto.getWeeks()) {
            tp.append(s);
            tp.append(" ");
        }
        a.setTimePattern(tp.toString());
        assignmentDAO.update(a);
        deleteEventsByAssId(a.getId());
        generateEventsByAssId(a.getId());
    }

    /**
     * Delete Assignment through dao and
     * delete all Events generated by this assignment.
     *
     * @param assignmentId
     */
    @Override
    @Transactional
    public void deleteById(Long assignmentId) {
        eventDAO.deleteByAssignmentId(assignmentId);
        assignmentDAO.delete(assignmentId);
    }

    /**
     * Return patient's id
     * of assignment id.
     *
     * @param assId
     * @return id
     */
    @Override
    @Transactional
    public Long getPatientId(Long assId) {
        Assignment assignment = assignmentDAO.getById(assId);
        Patient patient = assignment.getPatient();
        return patient.getId();
    }

    @Override
    @Transactional
    /**
     *get All assignments by Assignment id
     *
     */
    public List<Assignment> getAll(Long id) {
        return assignmentDAO.getAll(id);
    }

    @Override
    @Transactional
    /**
     * Get Assignment from dao and
     * convert AssignmentDto to Assignment
     *
     */
    public AssignmentDto getById(Long id) {
        Assignment ass = assignmentDAO.getById(id);
        AssignmentDto assignmentDto = new AssignmentDto();
        assignmentDto.setId(ass.getId());
        assignmentDto.setName(ass.getName());
        assignmentDto.setType(ass.getType());
        assignmentDto.setPeriod(ass.getTimePattern());
        assignmentDto.setWeeks(ass.getTimePattern().split("  "));
        assignmentDto.setTime1(ass.getTime1());
        assignmentDto.setTime2(ass.getTime2());
        assignmentDto.setTime3(ass.getTime3());
        assignmentDto.setDateFrom(ass.getDateFrom());
        assignmentDto.setDateTo(ass.getDateTo());
        assignmentDto.setDoze(ass.getDoze());
        return assignmentDto;
    }

    /**
     * Delete Assignment by Id
     *
     * @param assId
     */
    @Override
    public void deleteEventsByAssId(Long assId) {
        eventDAO.deleteByAssignmentId(assId);
    }

    /**
     * Generate events by Assignment id.
     *
     * @param assId
     */
    @Transactional
    @Override
    public void generateEventsByAssId(Long assId) {
        Assignment a = assignmentDAO.getById(assId);
        Patient patient = a.getPatient();
        List<String> weeks = Arrays.asList(a.getTimePattern().split(" "));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-d");
        LocalDate startDate = LocalDate.parse(a.getDateFrom(), formatter);
        LocalDate endDate = LocalDate.parse(a.getDateTo(), formatter);
        //идем по всем дням от и до
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            //если в этом периоде день недели совпадает с нужным
            if (weeks.contains(String.valueOf(date.getDayOfWeek().getValue()))) {
                if (a.getTime1() != null) {
                    Event event = new Event();
                    event.setAssignment(a);
                    event.setPatientName(patient.getLastName());
                    event.setStatus("Sheduled");
                    event.setComments("reason");
                    event.setDate(date.toString());
                    event.setTime(a.getTime1());
                    eventDAO.addEvent(event);
                }
                if (a.getTime2() != null) {
                    Event event = new Event();
                    event.setAssignment(a);
                    event.setPatientName(patient.getLastName());
                    event.setStatus("Sheduled");
                    event.setComments("reason");
                    event.setDate(date.toString());
                    event.setTime(a.getTime2());
                    eventDAO.addEvent(event);
                }
                if (a.getTime3() != null) {
                    Event event = new Event();
                    event.setAssignment(a);
                    event.setPatientName(patient.getLastName());
                    event.setStatus("Sheduled");
                    event.setComments("reason");
                    event.setDate(date.toString());
                    event.setTime(a.getTime3());
                    eventDAO.addEvent(event);
                }
            }
        }
    }
}



