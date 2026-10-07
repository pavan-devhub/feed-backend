package com.feedstartup.service.impl;

import com.feedstartup.dto.UserTypeDto;
import com.feedstartup.model.UserType;
import com.feedstartup.repository.UserTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Registration accepts exactly the active user_types rows, stored under their canonical names. */
class UserTypeServiceImplTest {

    private final UserTypeRepository repository = mock(UserTypeRepository.class);
    private final UserTypeServiceImpl service = new UserTypeServiceImpl(repository);

    private final UserType student = new UserType("Student", 3);
    private final UserType businessCollaborator = new UserType("Business Collaborator", 4);
    private final UserType retired = new UserType("Retired Type", 9);

    @BeforeEach
    void setUp() {
        retired.setActive(false);
        List<UserType> all = List.of(student, businessCollaborator, retired);
        when(repository.findByActiveTrueOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(student, businessCollaborator));
        when(repository.findByNameIgnoreCase(anyString())).thenAnswer(inv -> all.stream()
                .filter(t -> t.getName().equalsIgnoreCase(inv.getArgument(0)))
                .findFirst());
    }

    @Test
    void listsTheActiveTypesInOrder() {
        assertEquals(List.of(new UserTypeDto("Student"), new UserTypeDto("Business Collaborator")), service.listActive());
    }

    @Test
    void acceptsAnActiveTypeWhateverItsCaseOrSurroundingSpaces() {
        assertEquals("Student", service.resolveActive("Student"));
        assertEquals("Student", service.resolveActive("  student "));
        assertEquals("Business Collaborator", service.resolveActive("BUSINESS COLLABORATOR"));
    }

    @Test
    void rejectsAnUnknownTypeAndListsTheChoices() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.resolveActive("Farmer"));
        assertTrue(e.getMessage().contains("Farmer"));
        assertTrue(e.getMessage().contains("Student, Business Collaborator"));
    }

    @Test
    void rejectsAnInactiveTypeAndABlankOne() {
        assertThrows(IllegalArgumentException.class, () -> service.resolveActive("Retired Type"));
        assertThrows(IllegalArgumentException.class, () -> service.resolveActive("  "));
        assertThrows(IllegalArgumentException.class, () -> service.resolveActive(null));
    }

    @Test
    void findsOnlyTypesTheEpmFormsOffer() {
        student.setEpmOrder(3);

        assertEquals(Optional.of(student), service.findEpmParticipantType(" student "));
        assertEquals(Optional.empty(), service.findEpmParticipantType("Business Collaborator"));
        assertEquals(Optional.empty(), service.findEpmParticipantType(null));
    }

    @Test
    void anEmptyTableAcceptsNothing() {
        when(repository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.resolveActive("Student"));
    }
}
