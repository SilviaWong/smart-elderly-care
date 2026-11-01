package com.example.elderlycare.modules.elder.service;

import com.example.elderlycare.modules.elder.dto.ContactDTO;
import com.example.elderlycare.modules.elder.vo.ContactVO;

import java.util.List;

public interface ElderContactService {

    List<ContactVO> listContacts(Long elderId);

    Long createContact(Long elderId, ContactDTO dto);

    void updateContact(Long elderId, Long contactId, ContactDTO dto);

    void deleteContact(Long elderId, Long contactId);
}
