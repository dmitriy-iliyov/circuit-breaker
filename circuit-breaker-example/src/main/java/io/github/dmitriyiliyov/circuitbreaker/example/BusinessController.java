package io.github.dmitriyiliyov.circuitbreaker.example;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BusinessController {

    private final BusinessService service;

    public BusinessController(BusinessService service) {
        this.service = service;
    }

    @PostMapping("/event")
    public ResponseEntity<?> sendEvent(@RequestParam(defaultValue = "false") boolean shouldReturn) {
        if (shouldReturn) {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(service.businessOpWithResult());
        }
        service.businessOp();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @PostMapping("/observable-exception")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendEventWithObservableException() {
        service.businessOpWithObservableException();
    }

    @PostMapping("/ignorable-exception")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendEventWithIgnorableException() {
        service.businessOpWithIgnorableException();
    }

    @GetMapping("/slow-request")
    public void slowRequest() throws InterruptedException {
        service.unexpectableSlowBusinessOp();
    }

    @GetMapping
    public BusinessEvent getEvent() {
        return service.businessGetOp();
    }
}
