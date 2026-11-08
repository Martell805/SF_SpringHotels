package com.sfhotels.booking.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfhotels.booking.client.dto.RemoteError;
import com.sfhotels.common.error.ApiException;
import com.sfhotels.common.error.NotFoundException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class HotelClientErrorHandler implements RestClient.ResponseSpec.ErrorHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String message = readMessage(response, status);
        throw switch (status) {
            case 409 -> new RoomUnavailableException(message);
            case 404 -> new NotFoundException(message);
            default -> new ApiException(HttpStatus.BAD_GATEWAY, "Hotel service rejected request: " + message);
        };
    }

    private String readMessage(ClientHttpResponse response, int status) {
        try {
            RemoteError error = objectMapper.readValue(response.getBody(), RemoteError.class);
            return error.message() != null ? error.message() : "HTTP " + status;
        } catch (IOException ex) {
            return "HTTP " + status;
        }
    }
}
