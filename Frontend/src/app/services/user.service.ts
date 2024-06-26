import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CookieService } from 'ngx-cookie-service';

@Injectable({
  providedIn: 'root',
})

export class UserService {
  private apiKey = 'http://localhost:8080/api/user';

  constructor(private http: HttpClient,private cookieService:CookieService) {}

  login(info: any): Observable<any> {
    return this.http.post<any>(`${this.apiKey}/login`, info);
  }

  register(info: any): Observable<any> {
    return this.http.post<any>(`${this.apiKey}/register`, info);
  }

  isLoggedIn(): string {
    const token=this.cookieService.get('token');
    if (token) {
      return token;
    }
    return "";
  }
  logout():any{
    this.cookieService.delete('token');
    return null;
  }
  downloadFile(username:string): Observable<any> {
    return this.http.get<any>(`${this.apiKey}/downloadUser/${username}`,  {
      responseType: 'blob' as 'json',
      observe: 'response'
    });
  }

  uploadFile(file: File,username:string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${this.apiKey}/uploadStats/${username}`, formData);
  }
  displayUser(username:string):Observable<any>{
    return this.http.get<any>(`${this.apiKey}/displayUser/${username}`);
  }

  getFriends(username:string):Observable<any>{
    return this.http.get<any>(`${this.apiKey}/friends/${username}`);
  }
}