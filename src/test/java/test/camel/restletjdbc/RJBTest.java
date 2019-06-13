package test.camel.restletjdbc;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;



@DisplayName("Тестирование camel-example-restlet-jdbc")
public class RJBTest extends BaseTest {
    /*
    Список тестов:
    1. получение списка всех persons
    2. добавление person
    3. изменение person
    4. получение инфо по конкретному человеку
    5. удаление person

    * */

    @Step("Получим массив всех карточек")
    private List<Person> getAllPersonArray(String response){
        Pattern pattern = Pattern.compile("ID=(\\d*), FIRSTNAME=([\\w\\d]*), LASTNAME=([\\w\\d]*)");
        Matcher matcher = pattern.matcher(response);
        List<Person> persons = new ArrayList<>();
        while (matcher.find()) {
            Person person = new Person();
            person.setFirstName(matcher.group(2));
            person.setLastName(matcher.group(3));
            person.setId(Integer.parseInt(matcher.group(1)));
            persons.add(person);
        }
        return persons;
    }

    @Step("Получаем список всех карточек")
    private List<Person> getPeople() {
        Response response = given()
                .contentType(ContentType.TEXT)
                .when()
                .get("/persons");
        assert(response.statusCode() == 200);
        return getAllPersonArray(response.asString());
    }


    @Step("Проверим, что новая карточка есть в списке")
    private boolean checkIsPersonPresent(Person person, String response){
        List<Person> persons = getAllPersonArray(response);
        for (Person p: persons) {
            if (p.equals(person))
                return true;
        }
        return false;
    }


    /** добавляем карточку, проверяем статус-код и то, что возвращенная строка содержит
     * указанные имя  и фамилию */

    @Step("Создаем пользователя")
    public void addSinglePerson(){
        Person person = new Person();
        person.setFirstName(String.format("firstName%s", (int) (Math.random() * 1000)));
        person.setLastName(String.format("lastName%s", (int) (Math.random() * 1000)));

        String expectedAnswer = MessageFormat.format("FIRSTNAME={0}, LASTNAME={1}}]", person.getFirstName(),
                person.getLastName());
        Response response = given()
                .when()
                .param("firstName", person.getFirstName())
                .param("lastName", person.getLastName())
//                .log().all()
                .post("/persons");

        assert(response.statusCode() == 200);
        String actualAnswer = response.asString();
        assert(actualAnswer.contains(expectedAnswer));
        assert(checkIsPersonPresent(person, response.asString()));
    }

    /* Получение списка всех
    * первым шагом добавляем человека, чтобы список точно был не пустой
    * */

    @Test
    @DisplayName("Список всех карточек")
    @Description("Отправляем запрос GET по адресу /rs/persons и проверяем, что список не пустой. " +
            "Перед этим добавляем карточку")
    public void getAllPersons() {
        addSinglePerson();
        List<Person> persons = getPeople();
        assert(persons.size() > 0);
    }

    @Test
    @DisplayName("Добавляем карточку")
    @Description("Отправляем запрос POST по адресу /rs/persons")
    public void addPerson(){
        /*
        * Добавляем карточку
         */

        List<Person> personBefore = getPeople();
        addSinglePerson();
        List<Person> personAfter = getPeople();
        assert(personBefore.size() == personAfter.size() - 1);
    }


    @Test
    @DisplayName("Обновить карточку")
    @Description("Отправляем запрос PUT по адресу /rs/persons/1")
    public void updateExistingPerson(){
        /*
         * меняем данные карточки
         */

        addSinglePerson();

        List<Person> persons = getPeople();
        Person updatedPerson = new Person();

        updatedPerson.setFirstName(String.format("firstName%s", (int) (Math.random() * 1000)));
        updatedPerson.setLastName(String.format("lastName%s", (int) (Math.random() * 1000)));

        Response response = given()
                .when()
                .param("firstName", updatedPerson.getFirstName())
                .param("lastName", updatedPerson.getLastName())
                .log().all()
                .put("/persons/".concat(String.valueOf(persons.get(0).getId())));

        assert(response.statusCode() == 200);
        Response r = given()
                .when()
                .get("/persons/".concat(String.valueOf(persons.get(0).getId())));
        assert(r.statusCode() == 200);
        List<Person> currentPerson = getAllPersonArray(r.asString());
        assert(currentPerson.get(0).equals(updatedPerson));
    }


    @Test
    @DisplayName("Получить карточку")
    @Description("Отправляем запрос GET по адресу /rs/persons/{число}. Возьмем реальный id, получив список карточек" +
            "и проверим, что там именно то, что в общем списке")
    public void getPersonInfo(){
        /*
        * получаем карточку
         */
        addSinglePerson();
        List<Person> existingPerson = getPeople();
        Response r = given()
                .when()
                .get("/persons/".concat(String.valueOf(existingPerson.get(0).getId())));
        assert(r.statusCode() == 200);
        List<Person> currentPerson = getAllPersonArray(r.asString());
        assert(currentPerson.size() == 1);
        assert(currentPerson.get(0).equals(existingPerson.get(0)));
    }


    @Test
    @DisplayName("Удаляем карточку")
    @Description("Отправляем запрос DELETE по адресу /rs/persons. Проверяем, что количество карточек уменьшилось на 1")
    public void deletePerson(){
        addSinglePerson();
        List<Person> personBefore = getPeople();
        Response r = given()
                .when()
                .delete("/persons/".concat(String.valueOf(personBefore.get(0).getId())));
        List<Person> personAfter = getPeople();
        assert(r.statusCode() == 200);
        assert(personAfter.size() == personBefore.size() - 1);
    }
}
