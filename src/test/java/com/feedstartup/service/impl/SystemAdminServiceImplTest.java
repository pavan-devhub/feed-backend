package com.feedstartup.service.impl;

import com.feedstartup.dto.SystemAdminDto;
import com.feedstartup.dto.SystemAdminRequestDto;
import com.feedstartup.exception.AlreadyTakenException;
import com.feedstartup.exception.ConflictException;
import com.feedstartup.exception.ForbiddenException;
import com.feedstartup.model.SystemAdmin;
import com.feedstartup.repository.SystemAdminRepository;
import com.feedstartup.repository.UserRepository;
import com.feedstartup.service.SystemAdminSessionService;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Admins are added with a username, mobile number and email nobody has, and log in with any of
 * them. Only the default admin removes them.
 */
class SystemAdminServiceImplTest {

    private final SystemAdminRepository adminRepository = mock(SystemAdminRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SystemAdminSessionService sessionService = mock(SystemAdminSessionService.class);
    private final SystemAdminServiceImpl service = new SystemAdminServiceImpl(adminRepository, userRepository, sessionService);

    private final SystemAdmin creator = admin(1L, "admin", "9000000001", "admin@feedworld.com", "Secret123");

    @Test
    void createsAnAdminWithAHashedPasswordAndRecordsWhoAddedIt() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(adminRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        SystemAdminDto saved = service.create(request(" ravi.k ", "9876543210", " Ravi@Example.com "), 1L);

        ArgumentCaptor<SystemAdmin> captor = ArgumentCaptor.forClass(SystemAdmin.class);
        verify(adminRepository).saveAndFlush(captor.capture());
        SystemAdmin stored = captor.getValue();
        assertEquals("ravi.k", stored.getUsername());
        assertEquals("ravi@example.com", stored.getEmail());
        assertTrue(BCrypt.checkpw("Passw0rd!", stored.getPassword()));
        assertEquals("admin", saved.createdBy());
        assertFalse(saved.defaultAdmin());
    }

    @Test
    void aTakenUsernameIsRejectedWithoutSayingWhoHasIt() {
        when(adminRepository.existsByUsernameIgnoreCase("admin")).thenReturn(true);

        AlreadyTakenException e = assertThrows(AlreadyTakenException.class,
                () -> service.create(request("admin", "9876543210", "new@example.com"), 1L));

        assertEquals(List.of("username"), e.getFields());
        assertEquals("That username is already taken - please choose another.", e.getMessage());
        verify(adminRepository, never()).saveAndFlush(any());
    }

    @Test
    void aMobileNumberOrEmailAUserHasIsTakenToo() {
        when(userRepository.existsByPhone("9876543210")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("ravi@example.com")).thenReturn(true);

        AlreadyTakenException e = assertThrows(AlreadyTakenException.class,
                () -> service.create(request("ravi", "9876543210", "Ravi@example.com"), 1L));

        assertEquals(List.of("mobileNumber", "email"), e.getFields());
        assertEquals("That mobile number and email are already taken - please choose others.", e.getMessage());
    }

    @Test
    void reportsEveryTakenFieldAtOnce() {
        when(adminRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(true);
        when(adminRepository.existsByMobileNumber(anyString())).thenReturn(true);
        when(adminRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        AlreadyTakenException e = assertThrows(AlreadyTakenException.class,
                () -> service.create(request("admin", "9000000001", "admin@feedworld.com"), 1L));

        assertEquals(List.of("username", "mobileNumber", "email"), e.getFields());
        assertEquals("That username, mobile number and email are already taken - please choose others.", e.getMessage());
    }

    @Test
    void logsInWithTheUsernameEmailOrMobileNumber() {
        when(adminRepository.findByUsernameIgnoreCase("ADMIN")).thenReturn(Optional.of(creator));
        when(adminRepository.findByEmailIgnoreCase("admin@feedworld.com")).thenReturn(Optional.of(creator));
        when(adminRepository.findByMobileNumber("9000000001")).thenReturn(Optional.of(creator));

        assertSame(creator, service.authenticate(" ADMIN ", "Secret123").orElseThrow());
        assertSame(creator, service.authenticate("admin@feedworld.com", "Secret123").orElseThrow());
        assertSame(creator, service.authenticate("9000000001", "Secret123").orElseThrow());
    }

    @Test
    void aWrongPasswordIsRefusedAndAnUnknownLoginIsLeftToUsers() {
        when(adminRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(creator));
        when(adminRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.authenticate("admin", "wrong"));
        assertEquals("Invalid password", e.getMessage());
        assertTrue(service.authenticate("someone@example.com", "whatever").isEmpty());
        assertTrue(service.authenticate("  ", "whatever").isEmpty());
    }

    @Test
    void theDefaultAdminRemovesAnAdminLogsThemOutAndHandsTheirAddsUp() {
        SystemAdmin ravi = admin(2L, "ravi", "9876543210", "ravi@example.com", "Passw0rd!");
        ravi.setCreatedBy(creator);
        SystemAdmin meena = admin(3L, "meena", "9876543211", "meena@example.com", "Passw0rd!");
        meena.setCreatedBy(ravi);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(adminRepository.findById(2L)).thenReturn(Optional.of(ravi));
        when(adminRepository.findByCreatedBy_Id(2L)).thenReturn(List.of(meena));

        service.delete(2L, 1L);

        // Meena still isn't a default admin - she's credited to whoever added Ravi.
        assertSame(creator, meena.getCreatedBy());
        verify(adminRepository).saveAll(List.of(meena));
        verify(sessionService).deleteAllForAdmin(2L);
        verify(adminRepository).delete(ravi);
    }

    @Test
    void anAddedAdminCannotRemoveAnyone() {
        SystemAdmin ravi = admin(2L, "ravi", "9876543210", "ravi@example.com", "Passw0rd!");
        ravi.setCreatedBy(creator);
        SystemAdmin meena = admin(3L, "meena", "9876543211", "meena@example.com", "Passw0rd!");
        meena.setCreatedBy(creator);
        when(adminRepository.findById(2L)).thenReturn(Optional.of(ravi));
        when(adminRepository.findById(3L)).thenReturn(Optional.of(meena));

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> service.delete(3L, 2L));

        assertEquals("Only the default admin can remove admins.", e.getMessage());
        verify(adminRepository, never()).delete(any());
        verifyNoInteractions(sessionService);
    }

    @Test
    void theDefaultAdminCannotBeRemoved() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(creator));

        ConflictException e = assertThrows(ConflictException.class, () -> service.delete(1L, 1L));

        assertEquals("The default admin can't be removed.", e.getMessage());
        verify(adminRepository, never()).delete(any());
        verifyNoInteractions(sessionService);
    }

    private static SystemAdminRequestDto request(String username, String mobileNumber, String email) {
        SystemAdminRequestDto dto = new SystemAdminRequestDto();
        dto.setUsername(username);
        dto.setMobileNumber(mobileNumber);
        dto.setEmail(email);
        dto.setPassword("Passw0rd!");
        return dto;
    }

    private static SystemAdmin admin(Long id, String username, String mobileNumber, String email, String password) {
        SystemAdmin admin = new SystemAdmin();
        admin.setId(id);
        admin.setUsername(username);
        admin.setMobileNumber(mobileNumber);
        admin.setEmail(email);
        admin.setPassword(BCrypt.hashpw(password, BCrypt.gensalt(4)));
        return admin;
    }
}
