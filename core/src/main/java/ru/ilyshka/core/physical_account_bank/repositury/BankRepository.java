package ru.ilyshka.core.physical_account_bank.repositury;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import ru.ilyshka.core.physical_account_bank.entity.Bank;

@Mapper
public interface BankRepository {


    @Insert("""
            INSERT INTO physical_account.bank(id, name)
                  VALUES (#{id}, #{name})
            ON CONFLICT (id)
            DO UPDATE SET
                name = EXCLUDED.name
            """)
    void save(Bank product);
}
