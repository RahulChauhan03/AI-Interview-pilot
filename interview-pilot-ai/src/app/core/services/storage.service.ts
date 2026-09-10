import { Injectable } from '@angular/core';
import { User } from '../../features/auth/models/user.model';
import { StorageKeyEnum } from '../enums/storage-key.enum';

@Injectable({
  providedIn: 'root',
})
export class StorageService {

  private readonly TOKEN_KEY = StorageKeyEnum.AccessToken;
  private readonly USER_KEY = StorageKeyEnum.CurrentUser;

  saveToken(token: string): void {
    sessionStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY);
  }

  removeToken(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
  }

  removeUser(): void {
    sessionStorage.removeItem(this.USER_KEY);
  }

  saveUser(user: User): void {
    sessionStorage.setItem(
      this.USER_KEY,
      JSON.stringify(user)
    );
  }

  getUser(): User | null {
    const user = sessionStorage.getItem(this.USER_KEY);

    if (!user) {
      return null;
    }

    try {
      return JSON.parse(user) as User;
    } catch {
      return null;
    }
  }

  clear(): void {
    this.removeToken();
    this.removeUser();
  }
}
