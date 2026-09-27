package com.neu.service;

import com.neu.dto.AddressBookDTO;
import com.neu.entity.AddressBook;

import java.util.List;

public interface AddressBookService {
    void add(AddressBookDTO addressBookDTO);

    List<AddressBook> list();

    AddressBook getById(Long id);

    void update(AddressBookDTO addressBookDTO);

    void deleteById(Long id);

    void setDefault(Long id);

    AddressBook getDefault();
}
