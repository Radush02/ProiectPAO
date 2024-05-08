import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CookieService } from 'ngx-cookie-service';
@Injectable({
    providedIn: 'root',
  })

export class ChatService {
    private apiKey='http://localhost:8080/api/chat';
    constructor(private http: HttpClient,private cookieService:CookieService) {}
    send(data:any,receiver:string): Observable<any> {
        return this.http.post<any>(`${this.apiKey}/send/${receiver}`,data);
    }
    receive(sender:string,receiver:string): Observable<any> {
        return this.http.get<any>(`${this.apiKey}/receive?senderName=${sender}&username=${receiver}`);
    }
}