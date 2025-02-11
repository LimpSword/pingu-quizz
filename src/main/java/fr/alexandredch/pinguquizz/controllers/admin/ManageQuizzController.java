package fr.alexandredch.pinguquizz.controllers.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.alexandredch.pinguquizz.models.Question;
import fr.alexandredch.pinguquizz.models.Quizz;
import fr.alexandredch.pinguquizz.models.User;
import fr.alexandredch.pinguquizz.repositories.QuizzRepository;
import fr.alexandredch.pinguquizz.service.storage.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/quizz")
public class ManageQuizzController {

    private final QuizzRepository quizzRepository;
    private final StorageService storageService;

    public ManageQuizzController(QuizzRepository quizzRepository, StorageService storageService) {
        this.quizzRepository = quizzRepository;
        this.storageService = storageService;
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Quizz> getAllQuizz() {
        return quizzRepository.findAll();
    }

    @GetMapping("/get/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Quizz> getQuizz(@PathVariable Long id) {
        return quizzRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createQuizz(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "question_images", required = false) List<MultipartFile> questionImages,
            @RequestParam(value = "answer_images", required = false) List<MultipartFile> answerImages,
            @RequestParam(value = "questions", required = false) String questionsJson) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        Quizz quizz = new Quizz();
        quizz.setAuthor(user);
        quizz.setName(name);
        quizz.setDescription(description);

        if (image != null && !image.isEmpty()) {
            String imageName = storageService.store(image);
            quizz.setImage(imageName);
            quizz.setOriginalImageName(image.getOriginalFilename());
        }

        List<Question> questions = parseQuestionsJson(questionsJson);
        quizz.setQuestions(questions);

        quizzRepository.save(quizz);

        return ResponseEntity.ok("Quiz created successfully");
    }

    private List<Question> parseQuestionsJson(String questionsJson) {
        ObjectMapper objectMapper = new ObjectMapper();

        // The returned JSON is a list of Question separated by commas, without the surrounding brackets
        questionsJson = '[' + questionsJson + ']';

        List<Question> questions = new ArrayList<>();
        try {
            questions = objectMapper.readValue(questionsJson, objectMapper.getTypeFactory().constructCollectionType(List.class, Question.class));
        } catch (Exception _) {
        }

        // TODO: Set question images and answer images

        return questions;
    }
}
