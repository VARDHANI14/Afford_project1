import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
public class AverageCalculatorController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Set<Integer> numberWindow = new LinkedHashSet<>();
    
    @Value("${window.size}")
    private int windowSize;

    @GetMapping("/numbers")
    public ResponseEntity<?> getNumbers(@RequestParam("numberid") String numberId) {
        String apiUrl = getApiUrl(numberId);

        long startTime = System.currentTimeMillis();
        List<Integer> numbersFromServer = fetchNumbersFromServer(apiUrl);
        long duration = System.currentTimeMillis() - startTime;

        if (numbersFromServer == null || duration > 500) {
            return ResponseEntity.status(504).body("Timeout or error fetching numbers from server");
        }

        Set<Integer> windowPrevState = new LinkedHashSet<>(numberWindow);

        // Update window with new unique numbers
        numberWindow.addAll(numbersFromServer);
        if (numberWindow.size() > windowSize) {
            List<Integer> windowList = new CopyOnWriteArrayList<>(numberWindow);
            numberWindow.clear();
            numberWindow.addAll(windowList.subList(windowList.size() - windowSize, windowList.size()));
        }

        double average = numberWindow.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        return ResponseEntity.ok(new ResponseData(numbersFromServer, windowPrevState, new LinkedHashSet<>(numberWindow), average));
    }

    private String getApiUrl(String numberId) {
        switch (numberId) {
            case "p":
                return "http://20.244.56.144/test/primes";
            case "f":
                return "http://20.244.56.144/test/fibo";
            case "e":
                return "http://20.244.56.144/test/even";
            case "r":
                return "http://20.244.56.144/test/rand";
            default:
                throw new IllegalArgumentException("Invalid number ID");
        }
    }

    private List<Integer> fetchNumbersFromServer(String apiUrl) {
        try {
            ResponseEntity<NumbersResponse> response = restTemplate.getForEntity(apiUrl, NumbersResponse.class);
            return response.getBody().getNumbers();
        } catch (Exception e) {
            return null;
        }
    }

    private static class ResponseData {
        private List<Integer> numbers;
        private Set<Integer> windowPrevState;
        private Set<Integer> windowCurrState;
        private double avg;

        public ResponseData(List<Integer> numbers, Set<Integer> windowPrevState, Set<Integer> windowCurrState, double avg) {
            this.numbers = numbers;
            this.windowPrevState = windowPrevState;
            this.windowCurrState = windowCurrState;
            this.avg = avg;
        }

        public List<Integer> getNumbers() {
            return numbers;
        }

        public Set<Integer> getWindowPrevState() {
            return windowPrevState;
        }

        public Set<Integer> getWindowCurrState() {
            return windowCurrState;
        }

        public double getAvg() {
            return avg;
        }
    }

    private static class NumbersResponse {
        private List<Integer> numbers;

        public List<Integer> getNumbers() {
            return numbers;
        }

        public void setNumbers(List<Integer> numbers) {
            this.numbers = numbers;
        }
    }
}
