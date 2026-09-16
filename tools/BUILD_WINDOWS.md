# Windows Builder build — Field TAK Hub 2.3.0 RC1

Requirements: Windows 10/11, .NET SDK 10.x, Visual Studio 2026 or compatible MSBuild tooling.

```powershell
dotnet restore apps/builder/FieldTakHub.Builder.sln
dotnet build apps/builder/FieldTakHub.Builder.sln -c Release
```

Output is under the normal `bin/Release` folders. No signing certificate is included.
