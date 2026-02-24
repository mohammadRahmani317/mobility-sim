package ir.srbiau.cloudsim.mobilitysim.controller;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class GreetingController {
    @GetMapping("/test/{name}")
    public void get(@PathVariable String name){
        String name1 = name;
    }

    private List<Person> persons = new ArrayList<>();

    @PostMapping("/person")
    public Person addPerson(@RequestBody Person person) {
        persons.add(person); // افزودن شخص به لیست
        return person; // برگشت اطلاعات شخص
    }


    @GetMapping("/persons")
    public List<Person> getPersonsByName() {
        // لیستی از اشخاص نمونه
        List<Person> persons = new ArrayList<>();
        persons.add(new Person("محمد", 28, "تهران"));
        persons.add(new Person("علی", 32, "اصفهان"));
        persons.add(new Person("سارا", 25, "شیراز"));
        persons.add(new Person("محمد", 35, "کرج"));


        return persons;
    }


}